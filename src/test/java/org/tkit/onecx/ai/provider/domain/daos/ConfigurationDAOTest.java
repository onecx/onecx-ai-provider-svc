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
class ConfigurationDAOTest {
    @Inject
    ConfigurationDAO dao;

    @InjectMock
    EntityManager em;

    @BeforeEach
    void beforeAll() {
        Mockito.when(em.getCriteriaBuilder()).thenThrow(new RuntimeException("Test technical error exception"));
    }

    @Test
    void methodExceptionTests() {
        methodExceptionTests(() -> dao.findByCriteria(null),
                ConfigurationDAO.ErrorKeys.ERROR_FIND_CONFIGURATIONS_BY_CRITERIA);
        methodExceptionTests(() -> dao.findByFilterKeyEmptyValue(null),
                ConfigurationDAO.ErrorKeys.ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY_EMPTY_VALUE);
        methodExceptionTests(() -> dao.findByFilterKey(null),
                ConfigurationDAO.ErrorKeys.ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY);
    }

    void methodExceptionTests(Executable fn, Enum<?> key) {
        var exc = Assertions.assertThrows(DAOException.class, fn);
        Assertions.assertEquals(key, exc.key);
    }
}
