package org.tkit.onecx.ai.provider.rs.internal.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupOrchestrationMode;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupResponseStrategy;

class AgentGroupMapperTest {

    private final AgentGroupMapper mapper = new AgentGroupMapperImpl();

    @Test
    void missingSettingsUseDefaults() {
        var group = new AgentGroup();
        group.setOrchestrationMode(null);
        group.setResponseStrategy(null);

        mapper.applyDefaults(group);

        assertThat(group.getOrchestrationMode()).isEqualTo(AgentGroupOrchestrationMode.LEAD_DELEGATES);
        assertThat(group.getResponseStrategy()).isEqualTo(AgentGroupResponseStrategy.LAST);
    }

    @Test
    void explicitSettingsArePreserved() {
        var group = new AgentGroup();
        group.setOrchestrationMode(AgentGroupOrchestrationMode.SUPERVISOR_ROUTED);
        group.setResponseStrategy(AgentGroupResponseStrategy.SCORED);

        mapper.applyDefaults(group);

        assertThat(group.getOrchestrationMode()).isEqualTo(AgentGroupOrchestrationMode.SUPERVISOR_ROUTED);
        assertThat(group.getResponseStrategy()).isEqualTo(AgentGroupResponseStrategy.SCORED);
    }
}