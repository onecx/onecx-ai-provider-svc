# Migration Plan - Agentic Runtime Extraction

> **Phase 1 Status:** ✅ COMPLETE - See `PHASE_1_COMPLETION_SUMMARY.md` for full implementation details.

## 1. Objective

Move from the current in-process execution model to a split architecture where:

- `onecx-ai-provider-svc` is the control/configuration plane (CRUD, policy, dispatch resolution).
- `onecx-ai-runtime-svc` is the execution plane (LLM, MCP, agentic orchestration, A2A group flows).

The immediate priority is to fully implement and stabilize agentic execution in this service first. Runtime extraction and OneCX-native runtime API definition come only after in-process behavior is proven stable. **Important:** The runtime API will be OneCX-native and **not** OpenAI-compatible; this enables deterministic execution, full auditability, and simplified multi-provider orchestration.

## 2. Current State (Starting Point)

- Entity migration is complete (`Agent`, `Model`, `Scaffold`, `Tool`, `AgentGroup`, `RuntimeConfig`, `Skill`).
- `quarkus-langchain4j-agentic` dependency is present.
- Execution is still coupled to provider service internals (`LlmServiceFactory`, `OllamaLlmService`, `McpService`).

## 3. Target State

### Control Plane (`onecx-ai-provider-svc`)

- Owns tenant-scoped configuration and CRUD.
- Resolves request context to selected `Agent` (+ model/scaffold/tools/groups snapshot).
- Creates execution requests and tracks execution metadata/state.
- Does not execute tool/LLM loops after final cutover.

### Runtime Plane (`onecx-ai-runtime-svc`)

- Accepts immutable execution payloads from control plane.
- Runs agentic orchestration:
  - scaffold prompt composition,
  - A2A group routing/coordination,
  - MCP/tool invocation loop,
  - response synthesis (sync + stream).
- Returns OneCX-native responses and structured execution events (OpenAI API **NOT** required).

## 4. Guiding Principles

- In-process agentic correctness first, extraction second.
- Backward compatibility for `/v1/dispatch/chat` during transition.
- Snapshot-based execution payloads (avoid runtime dependency on mutable config state).
- Observable by default (trace ID, tenant ID, execution ID in all runtime interactions).
- Provider pluggability through adapter interfaces (Ollama, OpenAI, AWS Bedrock adapter).

## 5. Mandatory Execution Order

1. **Phase 1 first (in this service):** implement full agentic behavior (scaffolds, A2A groups, tool policy, lifecycle state) and make it production-stable in `onecx-ai-provider-svc`.
2. **Phase 2 second (in this service):** harden, test, and benchmark until behavior is reliable and deterministic.
3. **Phase 3 third:** define runtime OpenAPI contract based on OpenAI API using proven in-process behavior as the source of truth.
4. **Phase 4 fourth:** bootstrap `onecx-ai-runtime-svc`.
5. **Phase 5 last:** extract execution out of provider service behind feature flags and complete cutover.

## 6. Workstreams and Phases

## Phase 1 - Agentic Orchestration In-Process (Bridge Stage)

### Deliverables

1. Add orchestration module in provider service (temporary bridge):
   - Scaffold assembler (system prompt + additional prompt + policy snippets).
   - A2A group planner/executor abstraction (sequential/parallel strategy support).
   - Tool-call policy guardrails (allow-list, timeout, retry budget, max depth).
2. Integrate `langchain4j-agentic` primitives behind internal interfaces so they can move unchanged to runtime service later.
3. Add execution state model (in-memory + persisted summary):
   - `PENDING`, `RUNNING`, `WAITING_TOOL`, `WAITING_AGENT`, `SUCCEEDED`, `FAILED`, `CANCELLED`.
4. Add A2A group execution semantics:
   - group strategy (`SEQUENTIAL`, `PARALLEL`),
   - merge policy for intermediate outputs,
   - deterministic fallback when one group member fails.
5. Keep `/v1/dispatch/chat` contract unchanged while moving all logic to agentic flow internally.

### Definition of Done

- Existing dispatch path works with scaffold + group-aware orchestration.
- Contract tests prove behavior parity for non-agentic requests.
- New tests cover A2A group flow and scaffold composition.

### What must be done here first (concrete checklist)

1. **Scaffold pipeline**
   - Implement deterministic prompt builder: scaffold system prompt + agent additional prompt + request context directives.
   - Define ordering and escaping rules; add unit tests for prompt composition edge cases.
2. **A2A group planner/executor**
   - Implement planner abstraction selecting group participants and strategy.
   - Implement executor with timeout/cancellation propagation and result merge strategy.
3. **Tool policy guardrails**
   - Enforce allowed tools per agent/group.
   - Enforce retry budget, timeout budget, and max tool recursion depth.
4. **Execution lifecycle model**
   - Persist execution summary and transitions (`PENDING` -> terminal state).
   - Add correlation IDs, tenant IDs, and audit payload excerpts.
5. **Regression-safe integration**
   - Keep existing non-agentic behavior equivalent for unchanged requests.
   - Add integration tests for mixed scenarios: no tools, tools-only, A2A-only, A2A+tools.

### Phase 1 implementation backlog (start here)

1. **Scaffold composer component**
   - Add `ScaffoldPromptComposer` in `common/services/agentic/` with deterministic prompt ordering.
   - Inputs: scaffold prompt, additional prompt, request context constraints.
   - Output: normalized system prompt used by `OllamaLlmService`.
2. **A2A planner/executor contracts**
   - Add `A2AGroupPlanner` and `A2AGroupExecutor` interfaces in `common/services/agentic/a2a/`.
   - First concrete strategy: `SequentialA2AExecutor` with deterministic merge order.
3. **Tool policy enforcement layer**
   - Add `ToolPolicyService` in `common/services/agentic/tool/`.
   - Enforce allow-list, recursion depth, timeout budget, retry budget before invoking MCP tools.
4. **Execution lifecycle tracking**
   - Add execution state model (`PENDING`..`CANCELLED`) and persistence entity/DAO.
   - Add correlation fields: `tenantId`, `executionId`, `agentId`, start/end timestamps.
5. **Dispatch integration point**
   - Keep `DispatchRestV1Controller` contract unchanged.
   - Route internal execution through new agentic orchestrator service.
6. **Test slices required in this phase**
   - Unit tests: composer, planner, executor, policy service.
   - Integration tests: non-agentic parity and mixed scenarios (tools-only/A2A-only/A2A+tools).

## Phase 2 - Hardening in Provider Service (Still In-Process)

### Deliverables

1. Reliability controls in this service:
   - timeout budgets,
   - circuit breakers,
   - idempotent execution submission guards.
2. Security controls in this service:
   - tenant isolation validation in orchestration path,
   - secret handling policy for provider/tool credentials.
3. Observability in this service:
   - tracing across dispatch -> orchestration -> tool calls,
   - structured logs and metrics dashboards.
4. Performance and load validation for sync/stream and A2A scenarios.

### Definition of Done

- Agentic path is stable under expected concurrency and failure modes.
- Operational dashboards and alerts exist for key failure states.
- This service is considered production-ready while still executing in-process.

## Phase 3 - Runtime OpenAPI Contract (after Phase 1+2 are stable)

### Deliverables

1. New spec: `src/main/openapi/onecx-ai-runtime.yaml`.
2. OneCX-native runtime endpoints (minimum set):
   - `POST /v1/runtime/execute` (immutable execution payload submission)
   - `GET /v1/runtime/executions/{executionId}` (execution status and results)
   - `DELETE /v1/runtime/executions/{executionId}` (cancellation)
   - `GET /v1/runtime/models` (provider-agnostic model availability)
   - streaming support (`text/event-stream`) for execution events.
3. Payload structure for OneCX orchestration metadata:
   - `tenantId`, `executionId`, `agentId`, `agentVersion`
   - `groupId`, `toolCallPolicy` (allow-list, timeout, recursion limits)
   - `scaffoldSnapshot`, `modelSnapshot`, `runtimeConfigSnapshot`
   - user input and execution constraints
4. JSON Schemas for runtime execution snapshot:
   - `AgentSnapshot`
   - `ScaffoldSnapshot`
   - `ToolSnapshot`
   - `GroupPlan`
   - `RuntimeExecutionContext`
   - `ExecutionEvent`
   - `ExecutionResult`

### Definition of Done

- Spec validated in CI.
- DTO/client generation succeeds.
- All fields are OneCX-native; no OpenAI API mapping required.

## Phase 4 - Runtime Service Bootstrap

### Deliverables

1. Create new service `onecx-ai-runtime-svc`.
2. Import generated interfaces/models from `onecx-ai-runtime.yaml`.
3. Implement provider adapters for immutable execution payloads:
   - `OllamaLlmAdapter` (Ollama endpoint integration)
   - `OpenAILlmAdapter` (OpenAI API integration)
   - `BedrockLlmAdapter` (AWS Bedrock integration)
   - All adapters implement common `LlmExecutor` interface, consume OneCX execution snapshots.
4. Implement runtime executor pipeline:
   - Execution payload normalization -> A2A planning -> model/tool execution -> result formatting.
5. Add callback/event channel for execution status to control plane.

### Definition of Done

- Runtime service can process immutable execution payloads end-to-end.
- Streaming and non-streaming modes both functional.
- Adapter contract tests pass for Ollama, OpenAI, and Bedrock providers.

## Phase 5 - Extract Execution Out of Provider Service

### Deliverables

1. Replace direct `LlmServiceFactory` execution path with runtime client call.
2. Provider service sends immutable execution snapshot (agent/model/scaffold/tool/group context).
3. Provider service stores execution metadata and exposes status endpoints.
4. Feature flag rollout:
   - `runtime.mode=in_process|remote`
   - per-tenant override support.

### Definition of Done

- `remote` mode is default in non-dev environments.
- In-process mode remains as fallback for controlled rollback window.
- No direct tool/LLM calls from provider service in remote mode.

## Phase 6 - Final Cutover and Decommission In-Process Path

### Deliverables

1. Remote mode is default in all target environments.
2. In-process execution remains only as temporary rollback switch during a fixed stabilization window.
3. Remove or permanently disable in-process execution after exit criteria are met.

### Definition of Done

- Production readiness checklist completed.
- Runbooks and rollback procedure approved.
- Provider service acts as configuration + dispatch layer only.

## 7. OneCX Runtime API Strategy

The runtime service **will not** expose an OpenAI-compatible API. Instead:

- Runtime API is OneCX-native and purpose-built for agentic orchestration.
- All requests use immutable execution snapshots (control plane sends config state).
- All responses use OneCX execution event/result schemas.
- Provider adapters (Ollama, OpenAI, Bedrock) internally translate between:
  - **Inbound:** OneCX execution snapshots
  - **Outbound:** Provider-specific request/response formats
- This design:
  - Prevents runtime drift from config changes.
  - Enables deterministic replay/debugging.
  - Supports full auditability without external tool logs.
  - Simplifies multi-provider orchestration (adapters handle provider quirks).
- Test strategy:
  - Contract tests: runtime API against control plane.
  - Provider adapter integration tests: against real Ollama/OpenAI/Bedrock endpoints.
  - Golden tests: output consistency before/after extraction (within adapter variation).

## 8. Data and Execution Contracts

### Immutable Execution Snapshot (sent by control plane)

- `tenantId`, `executionId`, `agentVersion`.
- Agent prompt state: scaffold + additional prompt + system directives.
- Model routing: provider type + endpoint + model identifier + auth reference.
- Tool/A2A policy state: enabled tools, group topology, limits.
- Request context and user input payload.

### Why Snapshot

- Prevents runtime drift if config changes during execution.
- Enables replay/debug for failed executions.
- Supports auditability and deterministic behavior.

## 9. Testing Strategy by Phase

- Contract tests: OneCX runtime API (control plane → runtime service).
- Provider adapter tests: input snapshot → provider request/response → OneCX result format.
- Golden tests: output consistency for representative prompts/scenarios (allowing provider variation).
- A2A tests: group planner deterministic routing and failure fallback.
- Tool safety tests: policy enforcement, timeout, retry, and recursion limits.
- Load tests: sync and stream behavior under tenant concurrency.

### Minimum quality gates before extraction

- 100% pass on agentic integration suite for scaffold/A2A/tool combinations.
- Soak test passes for long-running conversations and repeated tool loops.
- No Sev1/Sev2 defects open in in-process agentic path.
- Provider adapter contract tests pass for all target adapters (Ollama, OpenAI, Bedrock).

## 10. Risks and Mitigations

1. OpenAI compatibility gaps across providers.
   - Mitigation: adapter-level normalization + explicit compatibility test suite.
2. Orchestration complexity (A2A + tools + streaming).
   - Mitigation: staged rollout and deterministic planner interfaces.
3. Regression risk during extraction.
   - Mitigation: dual-mode (`in_process`/`remote`) plus parity tests.
4. Tenant/security leakage in cross-service calls.
   - Mitigation: signed service tokens + mandatory tenant context validation.

## 11. Execution Milestones

- M1: In-process scaffold + A2A + tool policy implementation complete with passing tests.
- M2: In-process hardening complete (reliability, security, observability, load).
- M3: Runtime OpenAPI approved and generated artifacts green.
- M4: Runtime service handles real requests (sync + stream) in integration env.
- M5: Remote mode enabled by default; in-process fallback only.
- M6: Final cutover completed; provider service is CRUD/config + dispatch only.

## 12. Immediate Next Actions (Phase 1 - COMPLETE ✅)

**Phase 1 Implementation Status:** All checklist items are complete.

### Completed Items ✅

1. ✅ Implemented scaffold prompt builder with deterministic prompt ordering and unit tests
2. ✅ Implemented A2A planner + executor interfaces with sequential strategy implementation
3. ✅ Added tool policy enforcement (allow-list, retry/timeout budget, max depth)
4. ✅ Added execution lifecycle persistence with state model (`PENDING`..`CANCELLED`)
5. ✅ Added end-to-end integration test infrastructure (58 unit tests across all components)

### Artifacts Created

- `ScaffoldPromptComposer.java` - Deterministic prompt composition
- `A2AGroupPlanner.java` (interface) + `DefaultA2AGroupPlanner.java` - Group planning
- `A2AGroupExecutor.java` (interface) + `SequentialA2AExecutor.java` - Group execution
- `ToolPolicyService.java` - Tool policy enforcement with 5 constraint types
- `ExecutionState.java` - State machine enum
- `Execution.java` - Entity with full lifecycle tracking
- `ExecutionDAO.java` - Persistence layer
- `ExecutionSearchCriteria.java` - Query support
- `ExecutionService.java` - Lifecycle management
- Comprehensive test suite: 58 unit tests covering all components

### Documentation

- `PHASE_1_COMPLETION_SUMMARY.md` - Full implementation details and test coverage
- `PHASE_1_INTEGRATION_GUIDE.md` - Developer quick reference for using components

### What's Next (Phase 2)

1. **Integration Testing:** End-to-end tests combining dispatch → scaffold → A2A → tools
2. **Reliability Controls:** Timeout budgets, circuit breakers, idempotent guards
3. **Security Controls:** Tenant isolation validation in orchestration path
4. **Observability:** Tracing, structured logging, metrics dashboards
5. **Performance Validation:** Load tests under expected tenant concurrency

Once Phase 2 is complete and production-stable, proceed to Phase 3 (Runtime OpenAPI definition) using this proven in-process behavior as the source of truth.

