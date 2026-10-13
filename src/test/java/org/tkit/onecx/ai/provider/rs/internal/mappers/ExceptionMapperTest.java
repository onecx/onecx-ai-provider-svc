package org.tkit.onecx.ai.provider.rs.internal.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.junit.jupiter.api.Test;
import org.tkit.quarkus.jpa.exceptions.ConstraintException;

class ExceptionMapperTest {

    private final ExceptionMapper internal = new ExceptionMapperImpl();
    private final org.tkit.onecx.ai.provider.rs.external.v1.mappers.ExceptionMapper external = new org.tkit.onecx.ai.provider.rs.external.v1.mappers.ExceptionMapperImpl();

    @Test
    void validationErrorsIncludePropertyPathAndMessage() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        var path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        var exception = new ConstraintViolationException("invalid request", Set.of(violation));

        for (var response : List.of(internal.constraint(exception), external.constraint(exception))) {
            assertThat(response.getStatus()).isEqualTo(400);
            var problem = response.getEntity();
            assertThat(problem.getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");
            assertThat(problem.getDetail()).isEqualTo("invalid request");
            assertThat(problem.getInvalidParams()).hasSize(1);
            assertThat(problem.getInvalidParams().get(0).getName()).isEqualTo("name");
            assertThat(problem.getInvalidParams().get(0).getMessage()).isEqualTo("must not be blank");
        }
    }

    @Test
    void persistenceConstraintErrorsIncludeKeyAndDetail() {
        var exception = mock(ConstraintException.class);
        when(exception.getMessageKey()).thenAnswer(invocation -> ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS);
        when(exception.getConstraints()).thenReturn("duplicate name");

        for (var response : List.of(internal.exception(exception), external.exception(exception))) {
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getEntity().getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");
            assertThat(response.getEntity().getDetail()).isEqualTo("duplicate name");
            assertThat(response.getEntity().getParams()).isEmpty();
        }
    }

    @Test
    void optimisticLockErrorsIncludeDetail() {
        var response = internal.optimisticLock(new OptimisticLockException("stale version"));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().getErrorCode()).isEqualTo("OPTIMISTIC_LOCK");
        assertThat(response.getEntity().getDetail()).isEqualTo("stale version");
    }

    @Test
    void parametersPreserveNullsAndStringifyValues() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("count", 42);
        params.put("optional", null);

        for (var mapped : List.of(internal.map(params), external.map(params))) {
            assertThat(mapped).hasSize(2);
            assertThat(mapped.get(0).getKey()).isEqualTo("count");
            assertThat(mapped.get(0).getValue()).isEqualTo("42");
            assertThat(mapped.get(1).getKey()).isEqualTo("optional");
            assertThat(mapped.get(1).getValue()).isNull();
        }
        assertThat(internal.map(null)).isEmpty();
        assertThat(external.map(null)).isEmpty();
    }
}