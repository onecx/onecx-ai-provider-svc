package org.tkit.onecx.ai.provider.domain.daos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.tkit.onecx.ai.provider.domain.models.DangerPattern;
import org.tkit.onecx.ai.provider.domain.models.enums.DangerLevel;
import org.tkit.quarkus.jpa.exceptions.DAOException;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DangerPatternDAOTest {

    @Inject
    DangerPatternDAO dao;

    @InjectMock
    EntityManager em;

    @BeforeEach
    void beforeEach() {
        Mockito.when(em.getCriteriaBuilder()).thenThrow(new RuntimeException("Test technical error exception"));
    }

    @Test
    void methodExceptionTests() {
        methodExceptionTests(() -> dao.findAllPatterns(),
                DangerPatternDAO.ErrorKeys.ERROR_FIND_ALL_PATTERNS);
    }

    void methodExceptionTests(Executable fn, Enum<?> key) {
        var exc = Assertions.assertThrows(DAOException.class, fn);
        Assertions.assertEquals(key, exc.key);
    }

    @Test
    void equals_returnsTrueForSameInstance_andFalseForNull() {
        var pattern = new DangerPattern();
        pattern.setId("dp-1");
        pattern.setPattern("delete");
        pattern.setDangerLevel(DangerLevel.DANGEROUS);

        assertThat(pattern.equals(pattern)).isTrue();
        assertThat(pattern.equals(null)).isFalse();
        assertThat(pattern.equals("not-a-pattern")).isFalse();
    }

    @Test
    void equals_returnsTrueForSameId_andFalseForDifferentId() {
        var p1 = new DangerPattern();
        p1.setId("dp-1");
        var p2 = new DangerPattern();
        p2.setId("dp-1");
        var p3 = new DangerPattern();
        p3.setId("dp-2");

        assertThat(p1.equals(p2)).isTrue();
        assertThat(p1.equals(p3)).isFalse();
    }

    @Test
    void hashCode_isConsistentWithEquals() {
        var p1 = new DangerPattern();
        p1.setId("dp-1");
        var p2 = new DangerPattern();
        p2.setId("dp-1");

        assertThat(p1).hasSameHashCodeAs(p2);
    }
}
