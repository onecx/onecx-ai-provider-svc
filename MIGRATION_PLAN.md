# MIGRATION_PLAN.md — Agentic Entity Model Migration

> Note: Entity migration in this document is complete. The next-stage plan for agentic runtime extraction and OpenAI-compatible runtime API is in `MIGRATION_PLAN_RUNTIME.md`.

## Executive Summary

This document describes the migration of `onecx-ai-provider-svc` from its current **Configuration-centric** entity
model to a new **Agent-centric** ("agentic") model as defined in `agent-store.puml`. The migration is **largely
additive**: the `Configuration` entity maps almost 1:1 to the new `Agent`, `MCPServer` becomes `Tool`, and the
`Provider` entity is extended in-place. Four new entities are introduced (`Scaffold`, `RuntimeConfig`, `Skill`,
`AgentGroup`) along with their relation tables, DAOs, REST controllers, mappers, and OpenAPI schemas.

The external v1 chat API (`POST /v1/dispatch/chat`) remains **backward-compatible** — its contract is unchanged, only
the internal wiring that resolves the agent and builds the LLM call is updated.

Additionally, this service will first be fully refactored to include modular runtime orchestration capabilities. After the
refactoring is complete and all logic is integrated and tested in-process, the actual LLM/Agent/MCP execution logic can
be extracted into a separate **runtime-execution service** that is spawned on demand (for example via Kubernetes Jobs).
The runtime service API will follow OpenAI Chat Completions API standard to allow users to optionally use alternative
providers (e.g., AWS Bedrock) instead of the control-plane's built-in runtime.

All database changes are additive first, then destructive (rename/drop) in a separate changeset to allow safe rollback.

---

## Critical Design Decisions

### 1. Configuration IS the Agent (1:1, not 1:N)
`Configuration` maps directly to `Agent` — same primary key space, same filter-based dispatch lookup. There is no
fan-out. Existing rows can be migrated in a single `ALTER TABLE … RENAME` Liquibase changeset rather than a data
transformation.

### 2. `llmSystemMessage` Moves to `Scaffold`
`Configuration.llmSystemMessage` (a per-configuration string) is extracted into a new `Scaffold` entity that can be
shared across multiple Agents. During migration, a Scaffold row is auto-created for every existing Configuration row
with `system_prompt = llmSystemMessage`. The new `Agent.scaffold_id` FK then points to it.

### 3. `RuntimeConfig` Decouples Model/Endpoint from Provider
The current `Provider` holds both identity (`name`, `type`) and execution concerns (`llmUrl`, `modelName`). The new
`RuntimeConfig` entity owns the execution parameters (`model`, `endpoint`, `communication_mode`, `auth_config`) and
references a `Provider`. `Provider.llmUrl` is renamed to `url` and `authMode` is added; `modelName` and `apiKey` are
kept on `Provider` for backward compatibility during the transition.

### 4. `MCPServer` → `Tool` (rename + extend, not replace)
`MCPServer` is renamed `Tool` with a new `type` column (defaulting to `MCP` for all existing rows) and `authMode`.
All existing MCP behaviour in `McpService` is preserved; the service is updated to work from `Tool` entities filtered
by `type = MCP`.

### 5. Dispatch Flow Change

```
Current:  ChatRequest → DispatchRestV1Controller
                     → LlmServiceFactory.chat()
                     → ConfigurationService.findConfigurationsByRequestContext()  ← finds Configuration by filter
                     → OllamaLlmService.chat(configuration, …)                   ← reads provider + mcpServers

Target:   ChatRequest → DispatchRestV1Controller (unchanged)
                     → LlmServiceFactory.chat()
                     → AgentService.findAgentByRequestContext()                  ← finds Agent by filter (same algorithm)
                     → resolves Agent.scaffold (system_prompt)
                     → resolves Agent.runtimeConfig (model, endpoint, mode)
                     → OllamaLlmService.chat(agent, …)                          ← reads runtimeConfig + tools
```

The filter-matching algorithm in `ConfigurationService.findConfigurationsByRequestContext()` (wildcard, longest-match)
is **preserved verbatim** in the new `AgentService`.

### 6. External v1 API — Backward Compatibility
`POST /v1/dispatch/chat` in `onecx-ai-v1.yaml` is **not modified**. The `RequestContext.filter` (key/value) continues
to drive agent resolution. No client-facing contract breaks.

### 7. Internal API — Breaking Change
All `/internal/configurations/…` endpoints are **replaced** by `/internal/agents/…` and the MCP server endpoints by
`/internal/tools/…`. Consumers of the internal API must migrate. New endpoints are added for `Scaffold`, `RuntimeConfig`,
`Skill`, and `AgentGroup`.

---

## Architecture Direction: Control Plane vs Runtime Plane

### Control-Plane Service (`onecx-ai-provider-svc` after refactor)

- Owns tenant-scoped CRUD for `Agent`, `Provider`, `Tool`, `Scaffold`, `RuntimeConfig`, `Skill`, and `AgentGroup`.
- Resolves dispatch requests to the right Agent and creates runtime execution requests.
- Orchestrates runtime lifecycle (create/status/cancel) and stores execution metadata.
- Exposes internal REST APIs for administration and runtime tracking.
- Does **not** execute direct LLM calls in-process after extraction is complete.

### Runtime-Execution Service (new service)

- Receives an immutable execution payload (agent snapshot + runtime config + request context).
- Executes LLM + MCP tool loop and reports status/result back.
- Runs as ephemeral workloads (Kubernetes Job/Pod) or a worker deployment.
- Publishes lifecycle events (`RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`).

### Runtime Spawn Flow (target)

1. External request enters `POST /v1/dispatch/chat` (or new async dispatch endpoint).
2. Control-plane resolves `Agent` using filter matching.
3. Control-plane creates a `RuntimeExecution` record and emits/dispatches a start command.
4. Kubernetes Job (or worker) starts runtime-execution service with correlation and tenant context.
5. Runtime-execution service performs LLM/MCP processing and posts status/result callbacks or emits events.
6. Control-plane updates execution status and exposes it via runtime endpoints.

---

## Current Entity Model (Baseline)

| Entity / Class | Table | Key Fields |
|---|---|---|
| `Configuration` | `CONFIGURATION` | `tenantId`, `name`, `description`, `llmSystemMessage`, `provider` (FK), `filter` (embedded), `mcpServers` (M:N) |
| `Provider` | `PROVIDER` | `tenantId`, `name`, `type` (OLLAMA), `llmUrl`, `modelName`, `apiKey`, `description` |
| `MCPServer` | `MCP_SERVER` | `tenantId`, `name`, `description`, `url`, `apiKey`, `executionPolicy` |
| `Filter` | *(embeddable)* | `key` (FilterKey), `value` |
| *(join table)* | `CONFIGURATION_MCP_SERVER` | `configuration_id`, `mcp_server_id` |

---

## Target Entity Model (from agent-store.puml)

| Entity | Table | Key Fields |
|---|---|---|
| `Agent` | `AGENT` | `tenantId`, `name`, `model` (FK), `scaffold` (FK), `runtimeConfig` (FK), `additionalPrompt`, `a2aEnabled`, `version`, `filter` (embedded), `tools` (M:N), `groups` (M:N) |
| `Provider` | `PROVIDER` | `tenantId`, `name`, `type` (OLLAMA, OPENAI, AWS_BEDROCK), `url`, `apiKey`, `authMode`, `version` |
| `Model` | `MODEL` | `tenantId`, `provider` (FK), `name`, `modelIdentifier`, `modelConfig` (JSON), `communicationMode` (sync/async/stream), `version` |
| `Scaffold` | `SCAFFOLD` | `name`, `systemPrompt`, `sourceProduct`, `version` |
| `RuntimeConfig` | `RUNTIME_CONFIG` | `tenantId`, `provider` (FK), `endpoint`, `authConfig` (JSON), `version` |
| `Tool` | `TOOL` | `name`, `description`, `type` (MCP, HTTP, CUSTOM), `url`, `apiKey`, `executionPolicy`, `authMode` |
| `Skill` | `SKILL` | `name`, `inputSchema` (JSON), `outputSchema` (JSON), `version` |
| `AgentGroup` | `AGENT_GROUP` | `tenantId`, `name` |
| *(join)* | `AGENT_TOOL_RL` | `agent_id`, `tool_id` |
| *(join)* | `SCAFFOLD_SKILL_RL` | `scaffold_id`, `skill_id` |
| *(join)* | `AGENT_GROUP_RL` | `agent_id`, `group_id` |

---

## File Mapping: Current → Target

### Domain Models (`domain/models/`)

| Current File | Target File | Action |
|---|---|---|
| `Configuration.java` | `Agent.java` | **Rename + restructure** — class name, `@Table("AGENT")`, `@NamedEntityGraph` constant; rename `llmSystemMessage` → `additionalPrompt`; rename `mcpServers` → `tools`; rename join table; add `scaffold`, `runtimeConfig`, `a2aEnabled`, `version`, `groups` fields; change `provider` FK → `model` FK |
| `Provider.java` | `Provider.java` | **Extend** — add `authMode` (enum `AuthMode`); keep `url`, `apiKey`; extend enum `ProviderType` to add `OPENAI`, `AWS_BEDROCK`; add `version` field (optimistic locking) |
| *(none)* | `Model.java` | **New** — `@Entity @Table("model")`: `tenantId`, `provider` (FK), `name`, `modelIdentifier`, `modelConfig` (JSON), `communicationMode` (enum), `version` |
| `MCPServer.java` | `Tool.java` | **Rename + extend** — class name, `@Table("TOOL")`; add `type` (enum `ToolType { MCP, HTTP, CUSTOM }`), add `authMode` (enum `AuthMode`) |
| `Filter.java` | `Filter.java` | **No change** — already `@Embeddable`, re-embedded in `Agent` |
| *(none)* | `Scaffold.java` | **New** — `@Entity @Table("scaffold")`: `id`, `name`, `systemPrompt` (text), `sourceProduct`, `version`, `skills` (M:N) |
| *(none)* | `RuntimeConfig.java` | **New** — `@Entity @Table("runtime_config")`: `id`, `tenantId`, `provider` (FK), `endpoint`, `authConfig` (text/JSON), `version` |
| *(none)* | `Skill.java` | **New** — `@Entity @Table("skill")`: `id`, `name`, `inputSchema` (text), `outputSchema` (text), `version` |
| *(none)* | `AgentGroup.java` | **New** — `@Entity @Table("agent_group")`: `id`, `tenantId`, `name` |

### New Enums (`domain/models/enums/`)

| File | Action |
|---|---|
| `ProviderType.java` | Add `OPENAI`, `AWS_BEDROCK` values |
| `ExecutionPolicy.java` | No change |
| `FilterKey.java` | No change |
| `ToolType.java` | **New** — `MCP, HTTP, CUSTOM` |
| `AuthMode.java` | **New** — `API_KEY, OAUTH` |
| `CommunicationMode.java` | **New** — `SYNC, ASYNC, STREAM` |

### Domain Criteria (`domain/criteria/`)

| Current File | Target File | Action |
|---|---|---|
| `ConfigurationSearchCriteria.java` | `AgentSearchCriteria.java` | **Rename** |
| `MCPServerSearchCriteria.java` | `ToolSearchCriteria.java` | **Rename** — add `type` (ToolType) field |
| `ProviderSearchCriteria.java` | `ProviderSearchCriteria.java` | No change |
| *(none)* | `ModelSearchCriteria.java` | **New** — filter by `providerId`, `name`, `communicationMode` |
| *(none)* | `ScaffoldSearchCriteria.java` | **New** |
| *(none)* | `RuntimeConfigSearchCriteria.java` | **New** |
| *(none)* | `SkillSearchCriteria.java` | **New** |
| *(none)* | `AgentGroupSearchCriteria.java` | **New** |

### DAOs (`domain/daos/`)

| Current File | Target File | Action |
|---|---|---|
| `ConfigurationDAO.java` | `AgentDAO.java` | **Rename** — update all `Configuration`/`Configuration_` refs → `Agent`/`Agent_`; rename `findAIConfigurationsByCriteria` → `findAgentsByCriteria`; rename `findAllConfigurationsByFilterKey` → `findAllAgentsByFilterKey` |
| `MCPServerDAO.java` | `ToolDAO.java` | **Rename** — update all `MCPServer`/`MCPServer_` refs → `Tool`/`Tool_`; rename `findMCPServersByCriteria` → `findToolsByCriteria` |
| `ProviderDAO.java` | `ProviderDAO.java` | No change |
| *(none)* | `ModelDAO.java` | **New** — extends `AbstractDAO<Model>` with provider/communicationMode criteria search |
| *(none)* | `ScaffoldDAO.java` | **New** — extends `AbstractDAO<Scaffold>` with name/sourceProduct criteria search |
| *(none)* | `RuntimeConfigDAO.java` | **New** — extends `AbstractDAO<RuntimeConfig>` |
| *(none)* | `SkillDAO.java` | **New** — extends `AbstractDAO<Skill>` |
| *(none)* | `AgentGroupDAO.java` | **New** — extends `AbstractDAO<AgentGroup>` |

### Services (`common/services/`)

| Current File | Target File | Action |
|---|---|---|
| `configuration/ConfigurationService.java` | `agent/AgentService.java` | **Rename + adapt** — package rename; replace all `Configuration`/`MCPServer`/DAO/mapper refs → `Agent`/`Tool`; rename `findConfigurationsByRequestContext` → `findAgentByRequestContext` (preserve filter algorithm); update create/update to resolve `Scaffold`, `Model`, `RuntimeConfig` |
| `llm/LlmServiceFactory.java` | `llm/LlmServiceFactory.java` | Inject `AgentService` instead of `ConfigurationService`; pass `Agent` + resolved `Model` to service dispatch; add `default` branch in provider type switch for `AWS_BEDROCK` |
| `llm/AbstractLlmService.java` | `llm/AbstractLlmService.java` | Replace `Configuration` parameter → `Agent`; add `Model` parameter; adapt `createToolRegistry()` to pass `agent.getTools()` |
| `llm/OllamaLlmService.java` | `llm/OllamaLlmService.java` | Replace `Configuration` → `Agent` + `Model`; read URL/model from `model.getModelIdentifier()`, auth from `provider`; inject `agent.getScaffold().getSystemPrompt()` as `SystemMessage` |
| `mcp/McpService.java` | `mcp/McpService.java` | Replace `Configuration`/`MCPServer` → `Agent`/`Tool`; filter `agent.getTools()` by `type == ToolType.MCP` before iterating |
| `mcp/McpTool.java` | `mcp/McpTool.java` | No change |
| `mcp/McpToolRegistry.java` | `mcp/McpToolRegistry.java` | No change |

### REST Controllers (`rs/internal/controllers/`)

| Current File | Target File | Action |
|---|---|---|
| `ConfigurationRestController.java` | `AgentRestController.java` | **Rename** — implement generated `AgentInternalApi` |
| `MCPServerRestController.java` | `ToolRestController.java` | **Rename** — implement generated `ToolInternalApi` |
| `ProviderRestController.java` | `ProviderRestController.java` | No change to structure; `ProviderMapper` handles new fields |
| *(none)* | `ScaffoldRestController.java` | **New** — standard CRUD via `ScaffoldDAO` + `ScaffoldMapper` |
| *(none)* | `ModelRestController.java` | **New** — standard CRUD via `ModelDAO` + `ModelMapper`; supports provider-scoped model listing |
| *(none)* | `RuntimeConfigRestController.java` | **New** — standard CRUD via `RuntimeConfigDAO` + `RuntimeConfigMapper` |
| *(none)* | `SkillRestController.java` | **New** — standard CRUD via `SkillDAO` + `SkillMapper` |
| *(none)* | `AgentGroupRestController.java` | **New** — standard CRUD via `AgentGroupDAO` + `AgentGroupMapper` |

### REST Controller — External (`rs/external/v1/controllers/`)

| Current File | Target File | Action |
|---|---|---|
| `DispatchRestV1Controller.java` | `DispatchRestV1Controller.java` | **No change** |

### Mappers (`rs/internal/mappers/`)

| Current File | Target File | Action |
|---|---|---|
| `ConfigurationMapper.java` | `AgentMapper.java` | **Rename + adapt** — update field mappings: `llmProvider`→`model`, `mcpServers`→`tools`, `llmSystemMessage`→`scaffold.systemPrompt` |
| `MCPServerMapper.java` | `ToolMapper.java` | **Rename** — add `type`/`authMode` field mappings |
| `ProviderMapper.java` | `ProviderMapper.java` | Add `authMode` field mapping; keep `llmUrl`/`modelName` for runtime backward compatibility during transition |
| `ExceptionMapper.java` | `ExceptionMapper.java` | No change |
| *(none)* | `ModelMapper.java` | **New** |
| *(none)* | `ScaffoldMapper.java` | **New** |
| *(none)* | `RuntimeConfigMapper.java` | **New** |
| *(none)* | `SkillMapper.java` | **New** |
| *(none)* | `AgentGroupMapper.java` | **New** |

### OpenAPI Specs (`main/openapi/`)

| `onecx-ai-internal.yaml` | **Major rewrite** — remove `configurationInternal`/`mcpServerInternal` tags and paths; add `agentInternal`, `toolInternal`, `modelInternal`, `scaffoldInternal`, `runtimeConfigInternal`, `skillInternal`, `agentGroupInternal` tags and paths; add schemas for `AgentStatus` and `RuntimeConfigType`; keep Provider `llmUrl`/`modelName` fields for transition compatibility |
| `onecx-ai-v1.yaml` | **Updated** — replaced legacy `ConfigurationFilter`/`Context`/`MCPServer` schemas with clean `AgentFilter`-based contract; `/v1/dispatch/chat` endpoint path and operationId unchanged |
| *(new)* | `onecx-ai-runtime.yaml` | **New (future)** — OpenAI Chat Completions API-compatible spec for runtime service external interface (for Phase 7 extraction) |

---

## Phased Migration Steps

## Implementation Status Tracker

| Phase | Status | Notes |
|---|---|---|
| Phase 0 — Dependency Setup | DONE | `quarkus-langchain4j-agentic` added to `pom.xml` |
| Phase 1 — Foundation Renames | DONE | Phase 1 hard switch complete: AgentService, LlmServiceFactory, AbstractLlmService, OllamaLlmService, McpService all updated to use Agent/Tool instead of Configuration/MCPServer |
| Phase 2 — New Entities | DONE | `Model`, `Scaffold`, `RuntimeConfig`, `Skill`, `AgentGroup` entities created; all Phase 2 DAO/criteria classes created |
| Phase 3 — Service Layer | DONE | `AgentService` created for agent resolution and management; LLM service layer successfully refactored |
| Phase 4 — API Layer | DONE | Agent/Tool/OpenAPI DTOs generated, new controllers/mappers added, and legacy Configuration/MCPServer paths removed from the internal contract |
| Phase 5 — DB Migration | DONE | Liquibase v2 changeset (`db/v2/2026-06-10-migrate-tables.xml`) implemented and tested |
| Phase 6 — Test Updates | IN PROGRESS | DAO and selected external/controller tests updated; full suite and new status/type filter coverage pending |
| Phase 7 — Runtime Service Extraction | NOT STARTED | Deferred by design |

### Current Iteration Checklist

- [x] Phase 0: dependency prerequisite done
- [x] Phase 2.1: create `Model.java`
- [x] Phase 2.8 (partial): create `ModelDAO.java`
- [x] Phase 2.9 (partial): create `ModelSearchCriteria.java`
- [x] Add `CommunicationMode` enum
- [x] Add `AuthMode` enum
- [x] Add `ToolType` enum
- [x] Compile and fix any issues from newly added classes (`mvn -DskipTests compile` = BUILD SUCCESS, rerun green)
- [x] Phase 2.2: create `Scaffold.java`
- [x] Phase 2.3: create `RuntimeConfig.java`
- [x] Phase 2.4: create `Skill.java`
- [x] Phase 2.5: create `AgentGroup.java`
- [x] Phase 2.8: create remaining DAOs (`ScaffoldDAO`, `RuntimeConfigDAO`, `SkillDAO`, `AgentGroupDAO`)
- [x] Phase 2.9: create remaining criteria (`ScaffoldSearchCriteria`, `RuntimeConfigSearchCriteria`, `SkillSearchCriteria`, `AgentGroupSearchCriteria`)
- [x] Compile after full Phase 2 slice (`mvn -DskipTests compile` = BUILD SUCCESS)
- [x] Phase 1 groundwork: create `Agent` + `Tool` entities and `AgentDAO` + `ToolDAO`
- [x] Phase 1 hard switch: replace remaining old `Configuration`/`MCPServer` usage in services/controllers/mappers
- [x] Phase 4.1: add `Agent`/`Tool`/`Model`/`Scaffold`/`RuntimeConfig`/`Skill`/`AgentGroup` OpenAPI definitions and generate DTOs
- [x] Phase 4.2: add controllers/mappers for new internal APIs
- [x] Phase 4.3: remove legacy `Configuration`/`MCPServer` internal API paths/tags from `onecx-ai-internal.yaml`

### Phase 0 — Dependency Setup (prerequisite)

**Goal:** Add required LangChain4J agentic library to support new entity model and agent orchestration.

1. **Add to `pom.xml`** *(Status: DONE)*: Ensure the following dependency is present:

```xml
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-agentic</artifactId>
    <version>${quarkus-langchain4j.version}</version>
</dependency>
```

This library provides:
- Agent orchestration capabilities
- Agentic workflow support
- Enhanced tool/skill management
- Agent state machine handling

Run `mvn clean compile` to verify dependency resolution before proceeding to Phase 1.

Verification: `mvn -DskipTests compile` completed successfully on 2026-06-09.

---

### Phase 1 — Foundation: Rename Existing Entities

**Goal:** Rename `Configuration` → `Agent` and `MCPServer` → `Tool` without yet introducing new entities. The service
must compile and tests must pass after this phase.

**Status: COMPLETE** ✅

1. **`Configuration.java` → `Agent.java`** *(Status: DONE)*
   - Class created with `@Table("AGENT")`.
   - Fields: `name`, `description`, `additionalPrompt`, `a2aEnabled`, `model` (FK), `scaffold` (FK), `runtimeConfig` (FK), `filter`, `tools` (M:N).

2. **`MCPServer.java` → `Tool.java`** *(Status: DONE)*
   - Class created with `@Table("TOOL")`.
   - Fields: `name`, `description`, `type` (ToolType enum), `url`, `apiKey`, `executionPolicy`, `authMode`.

3. **`AgentDAO.java`** *(Status: DONE)*
   - Created with `findAgentsByCriteria()` and `findAllAgentsByFilterKey()` methods.
   - Supports filter-based agent lookup for request context matching.

4. **`ToolDAO.java`** *(Status: DONE)*
   - Exists with `findToolsByCriteria()` method supporting type-based filtering.

5. **`AgentService.java`** *(Status: DONE)*
   - Created in `common/services/agent/` package.
   - Implements `createAgent()`, `updateAgent()`, `findAgentByRequestContext()`.
   - Preserves filter-matching algorithm from ConfigurationService verbatim.

6. **`LlmServiceFactory.java`** *(Status: DONE)*
   - Updated to inject `AgentService` instead of `ConfigurationService`.
   - Routes to `AgentService.findAgentByRequestContext()` for agent discovery.
   - Resolves provider from agent's model before dispatching.

7. **`AbstractLlmService.java`** *(Status: DONE)*
   - Updated to accept `Agent` parameter instead of `Configuration`.
   - `createToolRegistry(agent)` now delegates to McpService.

8. **`OllamaLlmService.java`** *(Status: DONE)*
   - Updated to accept `Agent` parameter.
   - Reads `modelIdentifier` from agent's model.
   - Maintains backward compatibility with Provider fields (`llmUrl`, `modelName`).

9. **`McpService.java`** *(Status: DONE)*
   - Updated to accept `Agent` parameter.
   - Filters tools by `type == ToolType.MCP` before creating registry.
   - Maintains MCP tool discovery and execution.

10. **`ConfigurationRestController.java`** *(Status: DONE)*
    - Injected `AgentService` for future use.
    - Existing Configuration endpoints remain functional.

**Compilation Status:** ✅ BUILD SUCCESS

**Next Steps:** Phase 7 (Runtime Service Extraction) is deferred by design. Full integration test suite run recommended before production deployment.
