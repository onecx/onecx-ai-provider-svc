package org.tkit.onecx.ai.provider.domain.daos;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.tkit.quarkus.jpa.exceptions.DAOException;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AgentMcpToolRuleDAOTest {

    @Inject
    AgentMcpToolRuleDAO dao;

    @InjectMock
    EntityManager em;

    @BeforeEach
    void beforeEach() {
        Mockito.when(em.getCriteriaBuilder()).thenThrow(new RuntimeException("Test technical error exception"));
    }

    @Test
    void methodExceptionTests() {
        methodExceptionTests(() -> dao.findByAgentId("a1"),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_AGENT_ID);
        methodExceptionTests(() -> dao.findByAgentAndToolId("a1", "t1"),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_AGENT_AND_TOOL_ID);
        methodExceptionTests(() -> dao.findByAgentAndToolIds("a1", List.of("t1")),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_AGENT_AND_TOOL_IDS);
        methodExceptionTests(() -> dao.findByAgentAndGlobalToolIds("a1", List.of("g1")),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_AGENT_AND_GLOBAL_TOOL_IDS);
        methodExceptionTests(() -> dao.deleteByAgentId("a1"),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_DELETE_RULES_BY_AGENT_ID);
        methodExceptionTests(() -> dao.deleteByAgentAndToolId("a1", "t1"),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_DELETE_RULES_BY_AGENT_AND_TOOL_ID);
        methodExceptionTests(() -> dao.deleteByToolId("t1"),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_DELETE_RULES_BY_TOOL_ID);
        methodExceptionTests(() -> dao.deleteByGlobalToolId("g1"),
                AgentMcpToolRuleDAO.ErrorKeys.ERROR_DELETE_RULES_BY_GLOBAL_TOOL_ID);
    }

    void methodExceptionTests(Executable fn, Enum<?> key) {
        var exc = Assertions.assertThrows(DAOException.class, fn);
        Assertions.assertEquals(key, exc.key);
    }
}
