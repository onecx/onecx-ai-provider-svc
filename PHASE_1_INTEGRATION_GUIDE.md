# Phase 1 Component Integration Guide

This guide provides quick reference for integrating Phase 1 agentic components into the dispatch flow.

## Quick Start: How to Use the New Components

### 1. Track Execution Lifecycle

```java
// In DispatchRestV1Controller or LlmServiceFactory
@Inject
ExecutionService executionService;

// When request arrives
Execution execution = executionService.createExecution(
    agent, 
    null, // groupId (optional)
    "request excerpt for audit trail"
);

try {
    executionService.startExecution(execution.getExecutionId());
    
    // ... orchestration logic ...
    
    executionService.succeedExecution(
        execution.getExecutionId(), 
        "response output"
    );
} catch (Exception e) {
    executionService.failExecution(
        execution.getExecutionId(), 
        e.getClass().getSimpleName(), 
        e.getMessage()
    );
    throw e;
}
```

### 2. Compose Scaffold Prompt

```java
// In OllamaLlmService or similar
@Inject
ScaffoldPromptComposer scaffoldComposer;

String systemPrompt = scaffoldComposer.compose(agent, chatRequestDTO);

// Now use systemPrompt in LLM call
// e.g., systemMessage = new SystemMessage(systemPrompt);
```

### 3. Plan and Execute A2A Groups

```java
// In LlmServiceFactory or orchestrator
@Inject
DefaultA2AGroupPlanner planner;

@Inject
SequentialA2AExecutor executor;

if (agent.getA2aEnabled()) {
    A2AExecutionPlan plan = planner.plan(agent);
    
    String mergedResponse = executor.execute(plan, (unit) -> {
        // For each unit in plan, invoke agent
        return invokeAgentInGroup(unit.groupId(), unit.groupName());
    });
}
```

### 4. Enforce Tool Policies

```java
// Before invoking any tool
@Inject
ToolPolicyService toolPolicy;

@Inject
ExecutionService executionService;

// Validate tool is in allow-list
if (!toolPolicy.isToolAllowed(agent, toolId)) {
    throw new SecurityException("Tool not allowed for agent");
}

// Check constraints before call
if (!toolPolicy.isWithinRecursionDepth(currentDepth, null)) {
    throw new IllegalStateException("Max recursion depth exceeded");
}

if (!toolPolicy.hasTimeRemaining(remainingMs, requiredMs)) {
    throw new TimeoutException("Not enough time budget");
}

if (!toolPolicy.hasRetriesRemaining(retriesLeft)) {
    throw new IllegalStateException("Retries exhausted");
}

// Track invocation
executionService.incrementToolCallCount(executionId);
```

### 5. Handle A2A Group Calls

```java
// Track A2A calls for metrics
@Inject
ExecutionService executionService;

// In A2A executor or planner
executionService.incrementAgentCallCount(executionId);
```

## Component Reference

### ScaffoldPromptComposer

**Purpose:** Deterministic system prompt composition  
**Location:** `common/services/agentic/ScaffoldPromptComposer`

```java
public String compose(Agent agent, ChatRequestDTOV1 chatRequestDTO)
```

**Combines:**
1. Agent's scaffold system prompt
2. Agent's additional prompt
3. Request context filter directive

**Returns:** Normalized, newline-separated prompt string

### DefaultA2AGroupPlanner

**Purpose:** Plan A2A group execution  
**Location:** `common/services/agentic/a2a/DefaultA2AGroupPlanner`

```java
public A2AExecutionPlan plan(Agent agent)
```

**Returns:** `A2AExecutionPlan` with:
- `strategy`: Execution strategy (SEQUENTIAL)
- `units`: List of `A2AExecutionUnit` (groupId, groupName) sorted deterministically

### SequentialA2AExecutor

**Purpose:** Execute A2A groups sequentially  
**Location:** `common/services/agentic/a2a/SequentialA2AExecutor`

```java
public String execute(A2AExecutionPlan plan, Function<A2AExecutionUnit, String> unitExecutor)
```

**Behavior:**
- Executes units in order
- Invokes `unitExecutor` function for each unit
- Collects successful responses
- Logs and skips failed units (graceful degradation)
- Returns merged responses (newline-separated)

### ToolPolicyService

**Purpose:** Tool call policy enforcement  
**Location:** `common/services/agentic/tool/ToolPolicyService`

**Methods:**
- `isToolAllowed(agent, toolId)` → boolean
- `getAllowedToolIds(agent)` → Set<String>
- `isWithinRecursionDepth(depth, maxDepth)` → boolean
- `hasTimeRemaining(remainingMs, requiredMs)` → boolean
- `hasRetriesRemaining(retriesLeft)` → boolean
- `getDefaultMaxRecursionDepth()` → int (10)
- `getDefaultTimeoutBudgetMs()` → long (300_000)
- `getDefaultRetryBudget()` → int (3)

### ExecutionService

**Purpose:** Execution lifecycle management  
**Location:** `common/services/execution/ExecutionService`

**State Transitions:**
```
PENDING → RUNNING → [WAITING_TOOL|WAITING_AGENT] → RUNNING → [SUCCEEDED|FAILED|CANCELLED]
```

**Key Methods:**
- `createExecution(agent, groupId, requestExcerpt)` → Execution
- `startExecution(executionId)` → Execution
- `waitForResource(executionId, waitState)` → Execution
- `resumeExecution(executionId)` → Execution
- `succeedExecution(executionId, result)` → Execution
- `failExecution(executionId, errorType, errorMessage)` → Execution
- `cancelExecution(executionId)` → Execution
- `incrementToolCallCount(executionId)` → Execution
- `incrementAgentCallCount(executionId)` → Execution
- `getExecution(executionId)` → Execution

## Data Models

### ExecutionState (Enum)

```
PENDING      - Queued, not started
RUNNING      - Active execution
WAITING_TOOL - Waiting for tool result
WAITING_AGENT - Waiting for agent completion
SUCCEEDED    - Terminal: success
FAILED       - Terminal: error
CANCELLED    - Terminal: user cancelled
```

### A2AExecutionPlan (Record)

```java
record A2AExecutionPlan(
    A2AStrategy strategy,           // SEQUENTIAL or PARALLEL
    List<A2AExecutionUnit> units    // Groups to execute
)
```

### A2AExecutionUnit (Record)

```java
record A2AExecutionUnit(
    String groupId,      // UUID for group
    String groupName     // Human-readable name
)
```

### Execution (Entity)

**Correlation Fields:**
- `executionId` - Unique ID for tracing
- `tenantId` - Multi-tenancy support
- `agentId` - Agent reference
- `groupId` - Optional group reference

**Lifecycle Fields:**
- `state` - Current execution state
- `startTime`, `endTime` - Timestamps
- `durationMs` - Duration in milliseconds

**Tracking Fields:**
- `toolCallCount` - Number of tool invocations
- `agentCallCount` - Number of A2A calls

**Result Fields:**
- `result` - Output/response
- `errorType`, `errorMessage` - Error details
- `requestExcerpt` - Audit trail

**Snapshot Fields:**
- `agentIdSnapshot`, `agentVersionSnapshot` - Agent state at execution time
- `groupNameSnapshot` - Group name at execution time

## Integration Patterns

### Pattern 1: Simple Execution Tracking

```java
public String executeAgent(Agent agent, ChatRequestDTOV1 request) {
    Execution exec = executionService.createExecution(agent, null, null);
    
    try {
        executionService.startExecution(exec.getExecutionId());
        String result = doExecution(agent, request);
        executionService.succeedExecution(exec.getExecutionId(), result);
        return result;
    } catch (Exception e) {
        executionService.failExecution(exec.getExecutionId(), 
            e.getClass().getSimpleName(), e.getMessage());
        throw e;
    }
}
```

### Pattern 2: Scaffold Composition

```java
private String buildSystemPrompt(Agent agent, ChatRequestDTOV1 request) {
    String prompt = scaffoldComposer.compose(agent, request);
    // Use as system message in LLM call
    return prompt;
}
```

### Pattern 3: A2A with Tool Tracking

```java
public String executeWithA2A(Agent agent, ChatRequestDTOV1 request) {
    Execution exec = executionService.createExecution(agent, null, null);
    
    try {
        executionService.startExecution(exec.getExecutionId());
        
        if (agent.getA2aEnabled()) {
            A2AExecutionPlan plan = planner.plan(agent);
            String result = executor.execute(plan, unit -> {
                executionService.incrementAgentCallCount(exec.getExecutionId());
                return invokeAgent(unit);
            });
            executionService.succeedExecution(exec.getExecutionId(), result);
            return result;
        }
        
        throw new IllegalStateException("A2A not enabled");
    } catch (Exception e) {
        executionService.failExecution(exec.getExecutionId(), 
            e.getClass().getSimpleName(), e.getMessage());
        throw e;
    }
}
```

### Pattern 4: Tool Policy Enforcement

```java
private void validateAndCallTool(Agent agent, Execution exec, 
        Tool tool, int recursionDepth, long remainingMs) {
    
    // Policy enforcement
    if (!toolPolicy.isToolAllowed(agent, tool.getId().toString())) {
        throw new SecurityException("Tool not allowed");
    }
    
    if (!toolPolicy.isWithinRecursionDepth(recursionDepth, null)) {
        throw new IllegalStateException("Recursion depth exceeded");
    }
    
    if (!toolPolicy.hasTimeRemaining(remainingMs, 5000)) {
        throw new TimeoutException("Insufficient time");
    }
    
    if (!toolPolicy.hasRetriesRemaining(retries)) {
        throw new IllegalStateException("No retries left");
    }
    
    // Execute tool
    executionService.incrementToolCallCount(exec.getExecutionId());
    invokeTool(agent, tool);
}
```

## Logging and Debugging

All components log at appropriate levels:
- **INFO:** Execution state transitions, plan creation
- **WARN:** Policy violations, failed A2A units
- **DEBUG:** Detailed prompt composition, allowed tool lookups

Example log output:
```
INFO Creating execution exec-a1b2c3d4e5f6g7h8 for agent 42 in tenant acme
INFO Started execution exec-a1b2c3d4e5f6g7h8
INFO Execution exec-a1b2c3d4e5f6g7h8 succeeded with duration 1234ms
WARN A2A unit execution failed for group 'backup-agent' and will be skipped
WARN Execution exec-a1b2c3d4e5f6g7h8 failed: type=TimeoutException, message=Tool timeout
```

## Testing Your Integration

Create integration tests that cover:

1. **Scaffold composition** with multiple prompt sources
2. **A2A execution** with successful and failing groups
3. **Tool policy validation** for all constraint types
4. **Execution lifecycle** with state transitions
5. **Error handling** with proper error capture

Example test pattern:

```java
@Test
void testExecutionWithA2AAndTools() {
    Agent agent = setupAgentWithA2A();
    ChatRequestDTOV1 request = setupRequest();
    
    String result = executionService.executeAgent(agent, request);
    
    Execution exec = executionService.getExecution(executionId);
    assertEquals(ExecutionState.SUCCEEDED, exec.getState());
    assertNotNull(exec.getStartTime());
    assertNotNull(exec.getEndTime());
    assertNotNull(exec.getDurationMs());
    assertTrue(exec.getToolCallCount() > 0);
    assertTrue(exec.getAgentCallCount() > 0);
}
```

---

**For detailed implementation, see:** `PHASE_1_COMPLETION_SUMMARY.md`

