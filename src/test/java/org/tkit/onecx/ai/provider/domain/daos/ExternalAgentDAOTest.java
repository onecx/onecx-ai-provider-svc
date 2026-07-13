package org.tkit.onecx.ai.provider.domain.daos;

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
class ExternalAgentDAOTest {

    @Inject
    ExternalAgentDAO dao;

    @InjectMock
    EntityManager em;

    @BeforeEach
    void beforeEach() {
        Mockito.when(em.getCriteriaBuilder()).thenThrow(new RuntimeException("Test technical error exception"));
    }

    @Test
    void methodExceptionTests() {
        methodExceptionTests(() -> dao.findExternalAgentsByCriteria(null),
                ExternalAgentDAO.ErrorKeys.ERROR_FIND_EXTERNAL_AGENTS_BY_CRITERIA);
        methodExceptionTests(() -> dao.findExternalAgentsByGroupId("null"),
                ExternalAgentDAO.ErrorKeys.ERROR_FIND_EXTERNAL_AGENTS_BY_GROUP_ID);
    }

    void methodExceptionTests(Executable fn, Enum<?> key) {
        var exc = Assertions.assertThrows(DAOException.class, fn);
        Assertions.assertEquals(key, exc.key);
    }
}
