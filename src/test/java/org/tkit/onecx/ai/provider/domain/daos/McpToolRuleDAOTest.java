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
class McpToolRuleDAOTest {

    @Inject
    McpToolRuleDAO dao;

    @InjectMock
    EntityManager em;

    @BeforeEach
    void beforeEach() {
        Mockito.when(em.getCriteriaBuilder()).thenThrow(new RuntimeException("Test technical error exception"));
    }

    @Test
    void methodExceptionTests() {
        methodExceptionTests(() -> dao.findByToolId("t1"),
                McpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_TOOL_ID);
        methodExceptionTests(() -> dao.findByGlobalToolId("g1"),
                McpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_GLOBAL_TOOL_ID);
        methodExceptionTests(() -> dao.findByToolIds(List.of("t1")),
                McpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_TOOL_IDS);
        methodExceptionTests(() -> dao.findByGlobalToolIds(List.of("g1")),
                McpToolRuleDAO.ErrorKeys.ERROR_FIND_RULES_BY_GLOBAL_TOOL_IDS);
        methodExceptionTests(() -> dao.findByToolIdAndToolName("t1", "x"),
                McpToolRuleDAO.ErrorKeys.ERROR_FIND_RULE_BY_TOOL_AND_NAME);
        methodExceptionTests(() -> dao.findByGlobalToolIdAndToolName("g1", "x"),
                McpToolRuleDAO.ErrorKeys.ERROR_FIND_RULE_BY_GLOBAL_TOOL_AND_NAME);
        methodExceptionTests(() -> dao.deleteByToolId("t1"),
                McpToolRuleDAO.ErrorKeys.ERROR_DELETE_RULES_BY_TOOL_ID);
        methodExceptionTests(() -> dao.deleteByGlobalToolId("g1"),
                McpToolRuleDAO.ErrorKeys.ERROR_DELETE_RULES_BY_GLOBAL_TOOL_ID);
    }

    void methodExceptionTests(Executable fn, Enum<?> key) {
        var exc = Assertions.assertThrows(DAOException.class, fn);
        Assertions.assertEquals(key, exc.key);
    }
}
