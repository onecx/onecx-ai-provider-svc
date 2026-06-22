//package org.tkit.onecx.ai.provider.common.services.agentic.tool;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.util.ArrayList;
//import java.util.Set;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.tkit.onecx.ai.provider.domain.models.Agent;
//import org.tkit.onecx.ai.provider.domain.models.Tool;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("ToolPolicyService Tests")
//class ToolPolicyServiceTest {
//
//    @InjectMocks
//    private ToolPolicyService service;
//
//    private Agent testAgent;
//    private Tool testTool1;
//    private Tool testTool2;
//
//    @BeforeEach
//    void setup() {
//        testTool1 = new Tool();
//        testTool1.setId(1L);
//        testTool1.setName("Tool1");
//
//        testTool2 = new Tool();
//        testTool2.setId(2L);
//        testTool2.setName("Tool2");
//
//        testAgent = new Agent();
//        testAgent.setName("TestAgent");
//        testAgent.setTools(new ArrayList<>());
//    }
//
//    @Test
//    @DisplayName("isToolAllowed returns true for tool in allow-list")
//    void testIsToolAllowed_WithAllowedTool() {
//        testAgent.getTools().add(testTool1);
//
//        assertTrue(service.isToolAllowed(testAgent, "1"));
//    }
//
//    @Test
//    @DisplayName("isToolAllowed returns false for tool not in allow-list")
//    void testIsToolAllowed_WithDisallowedTool() {
//        testAgent.getTools().add(testTool1);
//
//        assertFalse(service.isToolAllowed(testAgent, "2"));
//    }
//
//    @Test
//    @DisplayName("isToolAllowed returns false when agent is null")
//    void testIsToolAllowed_WithNullAgent() {
//        assertFalse(service.isToolAllowed(null, "1"));
//    }
//
//    @Test
//    @DisplayName("isToolAllowed returns false when tool ID is null")
//    void testIsToolAllowed_WithNullToolId() {
//        testAgent.getTools().add(testTool1);
//
//        assertFalse(service.isToolAllowed(testAgent, null));
//    }
//
//    @Test
//    @DisplayName("isToolAllowed returns false when tool ID is blank")
//    void testIsToolAllowed_WithBlankToolId() {
//        testAgent.getTools().add(testTool1);
//
//        assertFalse(service.isToolAllowed(testAgent, "   "));
//    }
//
//    @Test
//    @DisplayName("isToolAllowed returns false when agent has no tools")
//    void testIsToolAllowed_WithNoTools() {
//        testAgent.setTools(new ArrayList<>());
//
//        assertFalse(service.isToolAllowed(testAgent, "1"));
//    }
//
//    @Test
//    @DisplayName("isToolAllowed returns false when agent tools is null")
//    void testIsToolAllowed_WithNullTools() {
//        testAgent.setTools(null);
//
//        assertFalse(service.isToolAllowed(testAgent, "1"));
//    }
//
//    @Test
//    @DisplayName("getAllowedToolIds returns correct tool IDs")
//    void testGetAllowedToolIds() {
//        testAgent.getTools().add(testTool1);
//        testAgent.getTools().add(testTool2);
//
//        Set<String> allowed = service.getAllowedToolIds(testAgent);
//
//        assertEquals(2, allowed.size());
//        assertTrue(allowed.contains("1"));
//        assertTrue(allowed.contains("2"));
//    }
//
//    @Test
//    @DisplayName("getAllowedToolIds returns empty set when agent has no tools")
//    void testGetAllowedToolIds_WithNoTools() {
//        testAgent.setTools(new ArrayList<>());
//
//        Set<String> allowed = service.getAllowedToolIds(testAgent);
//
//        assertTrue(allowed.isEmpty());
//    }
//
//    @Test
//    @DisplayName("getAllowedToolIds returns empty set when agent is null")
//    void testGetAllowedToolIds_WithNullAgent() {
//        Set<String> allowed = service.getAllowedToolIds(null);
//
//        assertTrue(allowed.isEmpty());
//    }
//
//    @Test
//    @DisplayName("getAllowedToolIds returns empty set when tools is null")
//    void testGetAllowedToolIds_WithNullTools() {
//        testAgent.setTools(null);
//
//        Set<String> allowed = service.getAllowedToolIds(testAgent);
//
//        assertTrue(allowed.isEmpty());
//    }
//
//    @Test
//    @DisplayName("isWithinRecursionDepth returns true when depth is within limit")
//    void testIsWithinRecursionDepth_WithinLimit() {
//        assertTrue(service.isWithinRecursionDepth(5, 10));
//    }
//
//    @Test
//    @DisplayName("isWithinRecursionDepth returns false when depth exceeds limit")
//    void testIsWithinRecursionDepth_ExceedsLimit() {
//        assertFalse(service.isWithinRecursionDepth(15, 10));
//    }
//
//    @Test
//    @DisplayName("isWithinRecursionDepth uses default limit when max depth is null")
//    void testIsWithinRecursionDepth_WithDefaultLimit() {
//        assertTrue(service.isWithinRecursionDepth(5, null));
//        assertFalse(service.isWithinRecursionDepth(15, null));
//    }
//
//    @Test
//    @DisplayName("hasTimeRemaining returns true when sufficient time available")
//    void testHasTimeRemaining_SufficientTime() {
//        assertTrue(service.hasTimeRemaining(5000, 3000));
//    }
//
//    @Test
//    @DisplayName("hasTimeRemaining returns false when insufficient time")
//    void testHasTimeRemaining_InsufficientTime() {
//        assertFalse(service.hasTimeRemaining(1000, 3000));
//    }
//
//    @Test
//    @DisplayName("hasTimeRemaining returns true when time exactly matches")
//    void testHasTimeRemaining_ExactTime() {
//        assertTrue(service.hasTimeRemaining(3000, 3000));
//    }
//
//    @Test
//    @DisplayName("hasRetriesRemaining returns true when retries available")
//    void testHasRetriesRemaining_TriesAvailable() {
//        assertTrue(service.hasRetriesRemaining(3));
//    }
//
//    @Test
//    @DisplayName("hasRetriesRemaining returns false when no retries available")
//    void testHasRetriesRemaining_NoTriesAvailable() {
//        assertFalse(service.hasRetriesRemaining(0));
//        assertFalse(service.hasRetriesRemaining(-1));
//    }
//
//    @Test
//    @DisplayName("Default constants are correctly set")
//    void testDefaultConstants() {
//        assertEquals(10, service.getDefaultMaxRecursionDepth());
//        assertEquals(300_000, service.getDefaultTimeoutBudgetMs());
//        assertEquals(3, service.getDefaultRetryBudget());
//    }
//
//    @Test
//    @DisplayName("getAllowedToolIds filters out null tool IDs")
//    void testGetAllowedToolIds_WithNullToolId() {
//        Tool nullIdTool = new Tool();
//        nullIdTool.setId(null);
//        testAgent.getTools().add(nullIdTool);
//        testAgent.getTools().add(testTool1);
//
//        Set<String> allowed = service.getAllowedToolIds(testAgent);
//
//        assertEquals(1, allowed.size());
//        assertTrue(allowed.contains("1"));
//    }
//}
