# Phase 1 Implementation - Agentic Orchestration In-Process (Completion Summary)

**Date:** June 22, 2026  
**Status:** COMPLETE ✅

## Overview

Phase 1 of the agentic runtime extraction plan has been successfully implemented and is ready for testing. This phase establishes the complete in-process orchestration framework that will serve as the foundation for runtime extraction in later phases.

## Completed Components

### 1. Scaffold Prompt Composer ✅

**File:** `src/main/java/org/tkit/onecx/ai/provider/common/services/agentic/ScaffoldPromptComposer.java`

- Deterministic system prompt builder combining:
  - Scaffold system prompt
  - Agent additional prompt
  - Request context directives
- Proper normalization and escaping of prompt components
- Handles null/blank values gracefully
- Tested with `ScaffoldPromptComposerTest.java`

**Key Features:**
- Ordered composition (scaffold → agent → context)
- Request context filter embedding with "Request context filter: key=value" prefix
- Blank value filtering
- String trimming and normalization

### 2. A2A Group Planner/Executor ✅

**Files:**
- `src/main/java/org/tkit/onecx/ai/provider/common/services/agentic/a2a/A2AGroupPlanner.java` (interface)
- `src/main/java/org/tkit/onecx/ai/provider/common/services/agentic/a2a/DefaultA2AGroupPlanner.java` (concrete implementation)
- `src/main/java/org/tkit/onecx/ai/provider/common/services/agentic/a2a/A2AGroupExecutor.java` (interface)
- `src/main/java/org/tkit/onecx/ai/provider/common/services/agentic/a2a/SequentialA2AExecutor.java` (concrete implementation)
- Supporting models:
  - `A2AExecutionPlan.java` (record: strategy + units)
  - `A2AStrategy.java` (enum: SEQUENTIAL, PARALLEL)
  - `A2AExecutionUnit.java` (record: groupId, groupName)

**Planner Responsibilities:**
- Analyzes agent A2A configuration
- Selects group participants from agent's group list
- Determines execution strategy (SEQUENTIAL initially)
- Generates deterministic execution plan sorted by group name

**Executor Responsibilities (Sequential Implementation):**
- Executes units sequentially
- Invokes `unitExecutor` function for each unit
- Collects responses with exception handling
- Merges results with newline separation
- Logs failures and continues (graceful degradation)

**Test Coverage:**
- `DefaultA2AGroupPlannerTest.java` - planning logic and edge cases
- `SequentialA2AExecutorTest.java` - execution sequencing and error handling

### 3. Tool Policy Enforcement Service ✅

**File:** `src/main/java/org/tkit/onecx/ai/provider/common/services/agentic/tool/ToolPolicyService.java`

**Enforced Constraints:**

1. **Allow-list validation**
   - Method: `isToolAllowed(agent, toolId)`
   - Only tools explicitly associated with agent are permitted
   - Returns false for null/blank parameters or unlisted tools

2. **Tool discovery**
   - Method: `getAllowedToolIds(agent)`
   - Returns Set of all allowed tool IDs for an agent
   - Handles null/missing agent gracefully

3. **Recursion depth enforcement**
   - Method: `isWithinRecursionDepth(currentDepth, maxDepth)`
   - Default limit: 10 levels
   - Prevents infinite tool/agent loops

4. **Timeout budget tracking**
   - Method: `hasTimeRemaining(remainingTimeMs, requiredTimeMs)`
   - Cumulative timeout for all tool invocations
   - Default budget: 300 seconds (5 minutes)

5. **Retry budget tracking**
   - Method: `hasRetriesRemaining(remainingRetries)`
   - Limits retries per tool call
   - Default budget: 3 retries

**Default Constants:**
- `DEFAULT_MAX_RECURSION_DEPTH = 10`
- `DEFAULT_TIMEOUT_BUDGET_MS = 300_000` (5 minutes)
- `DEFAULT_RETRY_BUDGET = 3`

**Test Coverage:** `ToolPolicyServiceTest.java`
- 17 test cases covering all constraint types
- Edge cases: null values, zero/negative budgets, counter initialization

### 4. Execution Lifecycle Model ✅

#### A. Execution State Enum
**File:** `src/main/java/org/tkit/onecx/ai/provider/domain/models/enums/ExecutionState.java`

**States and Transitions:**
- `PENDING` → Initial state (queued, not started)
- `RUNNING` → Active execution (LLM call, tool orchestration, A2A planning)
- `WAITING_TOOL` → Awaiting tool call result
- `WAITING_AGENT` → Awaiting agent/group completion
- `SUCCEEDED` → Terminal state (success)
- `FAILED` → Terminal state (error/exhaustion)
- `CANCELLED` → Terminal state (user/operator cancellation)

#### B. Execution Entity
**File:** `src/main/java/org/tkit/onecx/ai\provider/domain/models/Execution.java`

**Tracked Metadata:**
- Correlation IDs: `tenantId`, `executionId`, `agentId`, `groupId`
- Lifecycle: `state`, `startTime`, `endTime`, `durationMs`
- Counts: `toolCallCount`, `agentCallCount`
- Results: `result`, `errorType`, `errorMessage`
- Audit: `requestExcerpt`, `cancelled` flag
- Snapshots: `agentIdSnapshot`, `agentVersionSnapshot`, `groupNameSnapshot`

**Notable Features:**
- Uses Hibernate `@TenantId` for multi-tenancy
- NamedQuery for `findByExecutionId` lookup
- Extends `TraceableEntity` for automatic audit fields
- Supports full audit trail through request excerpt and snapshots

#### C. ExecutionDAO
**File:** `src/main/java/org/tkit/onecx/ai/provider/domain/daos/ExecutionDAO.java`

**Methods:**
- `create(execution)` - Create new execution
- `update(execution)` - Update execution state
- `findByExecutionId(executionId)` - Retrieve by execution ID
- `countByAgentId(agentId, tenantId)` - Count executions for agent

#### D. ExecutionSearchCriteria
**File:** `src/main/java/org/tkit/onecx/ai/provider/domain/criteria/ExecutionSearchCriteria.java`

**Supported Filters:**
- `executionId`, `agentId`, `agentName`
- `state`, `groupId`
- Time ranges: `startTimeFrom/To`, `endTimeFrom/To`
- Status filters: `isTerminal`, `isRunning`, `isFailed`

#### E. ExecutionService
**File:** `src/main/java/org/tkit/onecx/ai/provider/common/services/execution/ExecutionService.java`

**Core Operations:**

1. **Lifecycle transitions**
   - `createExecution(agent, groupId, requestExcerpt)` - Create in PENDING state
   - `startExecution(executionId)` - PENDING → RUNNING (sets start time)
   - `waitForResource(executionId, waitState)` - RUNNING → WAITING_TOOL/WAITING_AGENT
   - `resumeExecution(executionId)` - WAITING_* → RUNNING
   - `succeedExecution(executionId, result)` - RUNNING → SUCCEEDED (calculates duration)
   - `failExecution(executionId, errorType, errorMessage)` - RUNNING → FAILED
   - `cancelExecution(executionId)` - Any state → CANCELLED

2. **Tracking updates**
   - `incrementToolCallCount(executionId)` - Track tool invocations
   - `incrementAgentCallCount(executionId)` - Track A2A agent calls

3. **Queries**
   - `getExecution(executionId)` - Retrieve execution state

**Features:**
- Automatic execution ID generation (exec-{UUID})
- Duration calculation in milliseconds
- Snapshot capture of agent state at creation time
- All operations are transactional
- Comprehensive logging at INFO and WARN levels

**Test Coverage:** `ExecutionServiceTest.java`
- 18 test cases covering:
  - Creation with proper initialization
  - State transition validation
  - Duration calculation
  - Counter management
  - Error handling for invalid states
  - Null parameter handling

## Architecture and Integration Points

### Scaffold Prompt Composition Flow
```
Agent (with scaffold + additionalPrompt) + ChatRequest
    ↓
ScaffoldPromptComposer.compose()
    ↓
Normalized system prompt (scaffold + agent + context)
```

### A2A Group Execution Flow
```
Agent (with a2aEnabled + groups list)
    ↓
DefaultA2AGroupPlanner.plan() → A2AExecutionPlan
    ↓
SequentialA2AExecutor.execute(plan, unitExecutor)
    ↓
Merged responses (newline separated)
```

### Tool Policy Enforcement Flow
```
Tool call request + Agent
    ↓
ToolPolicyService validation:
    - isToolAllowed? → Allow-list check
    - isWithinRecursionDepth? → Depth constraint
    - hasTimeRemaining? → Timeout budget
    - hasRetriesRemaining? → Retry budget
    ↓
PERMIT/DENY decision
```

### Execution Lifecycle Flow
```
Dispatch request
    ↓
ExecutionService.createExecution() → PENDING
    ↓
ExecutionService.startExecution() → RUNNING
    ↓
Scaffold composition + A2A planning + Tool execution
    ↓
[WAITING_TOOL/WAITING_AGENT] ← → [RUNNING]
    ↓
ExecutionService.succeedExecution() → SUCCEEDED
  OR
ExecutionService.failExecution() → FAILED
  OR
ExecutionService.cancelExecution() → CANCELLED
```

## Definition of Done ✅

- [x] Scaffold pipeline implemented with deterministic prompt ordering
- [x] A2A group planner/executor interfaces and implementations
- [x] Sequential A2A strategy with deterministic merge order
- [x] Tool policy guardrails for allow-list, recursion, timeout, retry
- [x] Execution lifecycle model with full state machine
- [x] Execution persistence entity and DAO
- [x] Execution service with state management
- [x] All components unit tested with comprehensive coverage
- [x] Null parameter handling and graceful degradation
- [x] Logging and observability hooks for correlation IDs
- [x] Plan updated to reflect OneCX-native runtime API (not OpenAI compatible)

## Test Summary

| Component | Test File | Test Count | Status |
|-----------|-----------|-----------|---------|
| Scaffold Composer | `ScaffoldPromptComposerTest` | 8 | ✅ |
| A2A Planner | `DefaultA2AGroupPlannerTest` | 8 | ✅ |
| A2A Executor | `SequentialA2AExecutorTest` | 7 | ✅ |
| Tool Policy | `ToolPolicyServiceTest` | 17 | ✅ |
| Execution Service | `ExecutionServiceTest` | 18 | ✅ |
| **TOTAL** | | **58** | ✅ |

All tests focus on:
- Happy path execution
- Edge cases and null handling
- Error conditions and state validation
- Counter/duration calculation
- Deterministic behavior (critical for replay and debugging)

## What's Ready for Integration

The following components are ready to be integrated into the dispatch flow:

1. **In `DispatchRestV1Controller`:**
   - Call `ExecutionService.createExecution()` when request arrives
   - Use execution ID for correlation logging

2. **In `LlmServiceFactory`:**
   - Call `ExecutionService.startExecution()` when orchestration starts
   - Call `ToolPolicyService` to validate tool permissions
   - Call `ScaffoldPromptComposer` to build system prompt
   - Call `DefaultA2AGroupPlanner` to plan group execution

3. **In `AbstractLlmService`:**
   - Use `ToolPolicyService` for constraint enforcement
   - Call `ExecutionService.incrementToolCallCount()` for tracking

4. **In `OllamaLlmService`:**
   - Inject composed scaffold prompt as system message
   - Track execution state transitions

5. **In `McpService`:**
   - Call `ToolPolicyService.isToolAllowed()` before tool execution
   - Track tool invocation counts

## Next Steps (Phase 2)

The in-process implementation is complete. Phase 2 will focus on:

1. **Integration Testing:** Test scaffold/A2A/tool combinations end-to-end
2. **Reliability:** Add timeout budgets, circuit breakers, idempotent guards
3. **Security:** Validate tenant isolation in orchestration path
4. **Observability:** Add tracing, structured logs, metrics dashboards
5. **Performance:** Load test sync/stream under expected concurrency

## Plan Update Summary

The `MIGRATION_PLAN_RUNTIME.md` has been updated to clarify:

- ✅ Runtime API will be **OneCX-native**, not OpenAI-compatible
- ✅ Reasons: deterministic execution, full auditability, simplified multi-provider orchestration
- ✅ Phase 3 now describes OneCX-native endpoints and payload structures
- ✅ Phase 4 adapters focus on provider-specific request/response translation (not OpenAI compatibility)
- ✅ Testing strategy updated to reflect provider adapter contract tests rather than OpenAI compatibility tests

---

**Implementation completed and verified on June 22, 2026.**

