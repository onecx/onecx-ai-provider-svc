package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;

class DefaultA2AGroupPlannerTest {

    private final DefaultA2AGroupPlanner planner = new DefaultA2AGroupPlanner();

    @Test
    void plan_whenA2aDisabled_returnsEmptyPlan() {
        Agent agent = new Agent();
        agent.setA2aEnabled(false);

        A2AExecutionPlan plan = planner.plan(agent);

        assertThat(plan.strategy()).isEqualTo(A2AStrategy.SEQUENTIAL);
        assertThat(plan.units()).isEmpty();
    }

    @Test
    void plan_whenEnabled_sortsGroupsByName() {
        Agent agent = new Agent();
        agent.setA2aEnabled(true);

        AgentGroup zeta = new AgentGroup();
        zeta.setId("g-zeta");
        zeta.setName("zeta");

        AgentGroup alpha = new AgentGroup();
        alpha.setId("g-alpha");
        alpha.setName("alpha");

        agent.setGroups(Set.of(zeta, alpha));

        A2AExecutionPlan plan = planner.plan(agent);

        assertThat(plan.units()).hasSize(2);
        assertThat(plan.units().get(0).groupName()).isEqualTo("alpha");
        assertThat(plan.units().get(1).groupName()).isEqualTo("zeta");
    }
}
