//package org.tkit.onecx.ai.provider.common.services.execution;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//import java.time.OffsetDateTime;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.tkit.onecx.ai.provider.domain.daos.ExecutionDAO;
//import org.tkit.onecx.ai.provider.domain.models.Agent;
//import org.tkit.onecx.ai.provider.domain.models.Execution;
//import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("ExecutionService Tests")
//class ExecutionServiceTest {
//
//    @Mock
//    private ExecutionDAO executionDAO;
//
//    @InjectMocks
//    private ExecutionService service;
//
//    private Agent testAgent;
//
//    @BeforeEach
//    void setup() {
//        testAgent = new Agent();
//        testAgent.setId(1L);
//        testAgent.setName("TestAgent");
//        testAgent.setTenantId("tenant1");
//        testAgent.setVersion(1);
//    }
//
//    @Test
//    @DisplayName("createExecution initializes execution in PENDING state")
//    void testCreateExecution() {
//        doNothing().when(executionDAO).create(any(Execution.class));
//
//        Execution execution = service.createExecution(testAgent, "group1", "request excerpt");
//
//        assertNotNull(execution);
//        assertNotNull(execution.getExecutionId());
//        assertTrue(execution.getExecutionId().startsWith("exec-"));
//        assertEquals(ExecutionState.PENDING, execution.getState());
//        assertEquals("tenant1", execution.getTenantId());
//        assertEquals(testAgent, execution.getAgent());
//        assertEquals("1", execution.getAgentIdSnapshot());
//        assertEquals(1, execution.getAgentVersionSnapshot());
//        assertEquals("group1", execution.getGroupId());
//        assertEquals(0, execution.getToolCallCount());
//        assertEquals(0, execution.getAgentCallCount());
//        assertFalse(execution.getCancelled());
//        assertEquals("request excerpt", execution.getRequestExcerpt());
//        assertNull(execution.getStartTime());
//        assertNull(execution.getEndTime());
//
//        verify(executionDAO, times(1)).create(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("createExecution throws exception when agent is null")
//    void testCreateExecution_NullAgent() {
//        assertThrows(IllegalArgumentException.class, () -> {
//            service.createExecution(null, "group1", "excerpt");
//        });
//    }
//
//    @Test
//    @DisplayName("startExecution transitions from PENDING to RUNNING")
//    void testStartExecution() {
//        Execution execution = createTestExecution(ExecutionState.PENDING);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.startExecution("exec-123");
//
//        assertEquals(ExecutionState.RUNNING, result.getState());
//        assertNotNull(result.getStartTime());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("startExecution throws exception for non-PENDING state")
//    void testStartExecution_InvalidState() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//
//        assertThrows(IllegalStateException.class, () -> {
//            service.startExecution("exec-123");
//        });
//    }
//
//    @Test
//    @DisplayName("waitForResource transitions to WAITING_TOOL")
//    void testWaitForResource_WaitingTool() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.waitForResource("exec-123", ExecutionState.WAITING_TOOL);
//
//        assertEquals(ExecutionState.WAITING_TOOL, result.getState());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("waitForResource transitions to WAITING_AGENT")
//    void testWaitForResource_WaitingAgent() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.waitForResource("exec-123", ExecutionState.WAITING_AGENT);
//
//        assertEquals(ExecutionState.WAITING_AGENT, result.getState());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("resumeExecution transitions from WAITING to RUNNING")
//    void testResumeExecution() {
//        Execution execution = createTestExecution(ExecutionState.WAITING_TOOL);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.resumeExecution("exec-123");
//
//        assertEquals(ExecutionState.RUNNING, result.getState());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("succeedExecution sets SUCCEEDED state and calculates duration")
//    void testSucceedExecution() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        execution.setStartTime(OffsetDateTime.now().minusSeconds(5));
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.succeedExecution("exec-123", "result output");
//
//        assertEquals(ExecutionState.SUCCEEDED, result.getState());
//        assertNotNull(result.getEndTime());
//        assertEquals("result output", result.getResult());
//        assertNotNull(result.getDurationMs());
//        assertTrue(result.getDurationMs() >= 5000); // at least 5 seconds
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("failExecution sets FAILED state with error details")
//    void testFailExecution() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        execution.setStartTime(OffsetDateTime.now().minusSeconds(3));
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.failExecution("exec-123", "RuntimeException", "Tool call failed");
//
//        assertEquals(ExecutionState.FAILED, result.getState());
//        assertEquals("RuntimeException", result.getErrorType());
//        assertEquals("Tool call failed", result.getErrorMessage());
//        assertNotNull(result.getEndTime());
//        assertNotNull(result.getDurationMs());
//        assertTrue(result.getDurationMs() >= 3000); // at least 3 seconds
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("cancelExecution sets CANCELLED state")
//    void testCancelExecution() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        execution.setStartTime(OffsetDateTime.now().minusSeconds(2));
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.cancelExecution("exec-123");
//
//        assertEquals(ExecutionState.CANCELLED, result.getState());
//        assertTrue(result.getCancelled());
//        assertNotNull(result.getEndTime());
//        assertNotNull(result.getDurationMs());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("incrementToolCallCount increases counter")
//    void testIncrementToolCallCount() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        execution.setToolCallCount(5);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.incrementToolCallCount("exec-123");
//
//        assertEquals(6, result.getToolCallCount());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("incrementToolCallCount initializes counter if null")
//    void testIncrementToolCallCount_NullCounter() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        execution.setToolCallCount(null);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.incrementToolCallCount("exec-123");
//
//        assertEquals(1, result.getToolCallCount());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("incrementAgentCallCount increases counter")
//    void testIncrementAgentCallCount() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        execution.setAgentCallCount(3);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//        doNothing().when(executionDAO).update(any(Execution.class));
//
//        Execution result = service.incrementAgentCallCount("exec-123");
//
//        assertEquals(4, result.getAgentCallCount());
//        verify(executionDAO, times(1)).update(any(Execution.class));
//    }
//
//    @Test
//    @DisplayName("getExecution retrieves execution by ID")
//    void testGetExecution() {
//        Execution execution = createTestExecution(ExecutionState.RUNNING);
//        when(executionDAO.findByExecutionId("exec-123")).thenReturn(execution);
//
//        Execution result = service.getExecution("exec-123");
//
//        assertNotNull(result);
//        assertEquals(ExecutionState.RUNNING, result.getState());
//        verify(executionDAO, times(1)).findByExecutionId("exec-123");
//    }
//
//    @Test
//    @DisplayName("getExecution returns null when execution not found")
//    void testGetExecution_NotFound() {
//        when(executionDAO.findByExecutionId("exec-unknown")).thenReturn(null);
//
//        Execution result = service.getExecution("exec-unknown");
//
//        assertNull(result);
//    }
//
//    private Execution createTestExecution(ExecutionState state) {
//        Execution execution = new Execution();
//        execution.setId(1L);
//        execution.setExecutionId("exec-123");
//        execution.setTenantId("tenant1");
//        execution.setAgent(testAgent);
//        execution.setState(state);
//        execution.setToolCallCount(0);
//        execution.setAgentCallCount(0);
//        return execution;
//    }
//}
