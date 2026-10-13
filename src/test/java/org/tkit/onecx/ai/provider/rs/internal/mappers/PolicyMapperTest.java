package org.tkit.onecx.ai.provider.rs.internal.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionPolicy;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolPermission;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.ExecutionPolicyDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ToolPermissionDTO;

class PolicyMapperTest {

    private final ToolMapper toolMapper = new ToolMapperImpl();
    private final AgentMcpToolRuleMapper ruleMapper = new AgentMcpToolRuleMapperImpl();

    @Test
    void nullPoliciesUseAskDefaults() {
        assertThat(toolMapper.mapExecutionPolicy((ExecutionPolicyDTO) null)).isEqualTo(ExecutionPolicy.ALWAYS_ASK);
        assertThat(toolMapper.mapExecutionPolicy((ExecutionPolicy) null)).isEqualTo(ExecutionPolicyDTO.ALWAYS_ASK);
        assertThat(ruleMapper.mapToolPermission((ToolPermissionDTO) null)).isEqualTo(ToolPermission.ALWAYS_ASK);
        assertThat(ruleMapper.mapToolPermission((ToolPermission) null)).isEqualTo(ToolPermissionDTO.ALWAYS_ASK);
    }

    @Test
    void executionPolicyInputsAreCanonicalized() {
        for (var policy : ExecutionPolicyDTO.values()) {
            var expected = policy == ExecutionPolicyDTO.ALWAYS_ASK
                    ? ExecutionPolicy.ALWAYS_ASK
                    : ExecutionPolicy.ALWAYS_ALLOW;
            assertThat(toolMapper.mapExecutionPolicy(policy)).isEqualTo(expected);
        }
    }

    @Test
    void executionPolicyOutputsAreCanonicalized() {
        for (var policy : ExecutionPolicy.values()) {
            var expected = policy == ExecutionPolicy.ALWAYS_ASK
                    ? ExecutionPolicyDTO.ALWAYS_ASK
                    : ExecutionPolicyDTO.ALWAYS_ALLOW;
            assertThat(toolMapper.mapExecutionPolicy(policy)).isEqualTo(expected);
        }
    }

    @Test
    void permissionInputsAreCanonicalized() {
        for (var permission : ToolPermissionDTO.values()) {
            var expected = switch (permission) {
                case DENY -> ToolPermission.DENY;
                case ALWAYS_ASK -> ToolPermission.ALWAYS_ASK;
                default -> ToolPermission.ALWAYS_ALLOW;
            };
            assertThat(ruleMapper.mapToolPermission(permission)).isEqualTo(expected);
        }
    }

    @Test
    void permissionOutputsAreCanonicalized() {
        for (var permission : ToolPermission.values()) {
            var expected = switch (permission) {
                case DENY -> ToolPermissionDTO.DENY;
                case ALWAYS_ASK -> ToolPermissionDTO.ALWAYS_ASK;
                default -> ToolPermissionDTO.ALWAYS_ALLOW;
            };
            assertThat(ruleMapper.mapToolPermission(permission)).isEqualTo(expected);
        }
    }
}