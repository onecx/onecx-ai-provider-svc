package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.onecx.ai.provider.domain.models.Agent;

@ApplicationScoped
public class DefaultA2AGroupPlanner implements A2AGroupPlanner {

    @Override
    public A2AExecutionPlan plan(Agent agent) {
        if (agent == null || !Boolean.TRUE.equals(agent.getA2aEnabled()) || agent.getGroups() == null
                || agent.getGroups().isEmpty()) {
            return new A2AExecutionPlan(A2AStrategy.SEQUENTIAL, List.of());
        }

        List<A2AExecutionUnit> units = agent.getGroups().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(group -> safeString(group.getName())))
                .map(group -> new A2AExecutionUnit(
                        group.getId() != null ? group.getId().toString() : null,
                        group.getName()))
                .toList();

        return new A2AExecutionPlan(A2AStrategy.SEQUENTIAL, units);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
