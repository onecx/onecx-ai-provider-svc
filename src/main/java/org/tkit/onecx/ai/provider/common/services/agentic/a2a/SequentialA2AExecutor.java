package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

/**
 * Executes A2A units sequentially and merges successful results with newline separators.
 */
@Slf4j
@ApplicationScoped
public class SequentialA2AExecutor implements A2AGroupExecutor {

    @Override
    public String execute(A2AExecutionPlan plan, Function<A2AExecutionUnit, String> unitExecutor) {
        if (plan == null || plan.units() == null || plan.units().isEmpty() || unitExecutor == null) {
            return "";
        }

        List<String> results = new ArrayList<>();
        for (A2AExecutionUnit unit : plan.units()) {
            if (unit == null) {
                continue;
            }
            try {
                String result = unitExecutor.apply(unit);
                if (result != null && !result.isBlank()) {
                    results.add(result.trim());
                }
            } catch (Exception e) {
                log.warn("A2A unit execution failed for group '{}' and will be skipped", unit.groupName(), e);
            }
        }

        return results.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }
}
