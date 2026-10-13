package org.tkit.onecx.ai.provider.domain.models.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionPolicy;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolPermission;

class PolicyConvertersTest {

    private final ExecutionPolicyConverter executionPolicyConverter = new ExecutionPolicyConverter();
    private final ToolPermissionConverter toolPermissionConverter = new ToolPermissionConverter();

    @Test
    void executionPolicyConverterWritesCanonicalValues() {
        assertThat(executionPolicyConverter.convertToDatabaseColumn(ExecutionPolicy.ALWAYS_ASK)).isEqualTo("ALWAYS_ASK");
        assertThat(executionPolicyConverter.convertToDatabaseColumn(ExecutionPolicy.ALWAYS_ALLOW)).isEqualTo("ALWAYS_ALLOW");
        assertThat(executionPolicyConverter.convertToDatabaseColumn(ExecutionPolicy.NEVER_ASK)).isEqualTo("ALWAYS_ALLOW");
        assertThat(executionPolicyConverter.convertToDatabaseColumn(ExecutionPolicy.ALLOW)).isEqualTo("ALWAYS_ALLOW");
        assertThat(executionPolicyConverter.convertToDatabaseColumn(null)).isEqualTo("ALWAYS_ASK");
    }

    @Test
    void executionPolicyConverterReadsLegacyValues() {
        assertThat(executionPolicyConverter.convertToEntityAttribute(null)).isEqualTo(ExecutionPolicy.ALWAYS_ASK);
        assertThat(executionPolicyConverter.convertToEntityAttribute(" ")).isEqualTo(ExecutionPolicy.ALWAYS_ASK);
        assertThat(executionPolicyConverter.convertToEntityAttribute("ALWAYS_ASK")).isEqualTo(ExecutionPolicy.ALWAYS_ASK);
        assertThat(executionPolicyConverter.convertToEntityAttribute(" always_allow ")).isEqualTo(ExecutionPolicy.ALWAYS_ALLOW);
        assertThat(executionPolicyConverter.convertToEntityAttribute("NEVER_ASK")).isEqualTo(ExecutionPolicy.ALWAYS_ALLOW);
        assertThat(executionPolicyConverter.convertToEntityAttribute("ALLOW")).isEqualTo(ExecutionPolicy.ALWAYS_ALLOW);
        assertThat(executionPolicyConverter.convertToEntityAttribute("ALWAYS_ALLOW")).isEqualTo(ExecutionPolicy.ALWAYS_ALLOW);
        assertThat(executionPolicyConverter.convertToEntityAttribute("UNKNOWN")).isEqualTo(ExecutionPolicy.ALWAYS_ASK);
    }

    @Test
    void toolPermissionConverterWritesCanonicalValues() {
        assertThat(toolPermissionConverter.convertToDatabaseColumn(ToolPermission.DENY)).isEqualTo("DENY");
        assertThat(toolPermissionConverter.convertToDatabaseColumn(ToolPermission.ALWAYS_ASK)).isEqualTo("ALWAYS_ASK");
        assertThat(toolPermissionConverter.convertToDatabaseColumn(ToolPermission.ALWAYS_ALLOW)).isEqualTo("ALWAYS_ALLOW");
        assertThat(toolPermissionConverter.convertToDatabaseColumn(ToolPermission.NEVER_ASK)).isEqualTo("ALWAYS_ALLOW");
        assertThat(toolPermissionConverter.convertToDatabaseColumn(ToolPermission.ALLOW)).isEqualTo("ALWAYS_ALLOW");
        assertThat(toolPermissionConverter.convertToDatabaseColumn(null)).isEqualTo("ALWAYS_ASK");
    }

    @Test
    void toolPermissionConverterReadsLegacyValues() {
        assertThat(toolPermissionConverter.convertToEntityAttribute(null)).isEqualTo(ToolPermission.ALWAYS_ASK);
        assertThat(toolPermissionConverter.convertToEntityAttribute(" ")).isEqualTo(ToolPermission.ALWAYS_ASK);
        assertThat(toolPermissionConverter.convertToEntityAttribute("ALWAYS_ASK")).isEqualTo(ToolPermission.ALWAYS_ASK);
        assertThat(toolPermissionConverter.convertToEntityAttribute("DENY")).isEqualTo(ToolPermission.DENY);
        assertThat(toolPermissionConverter.convertToEntityAttribute(" always_allow ")).isEqualTo(ToolPermission.ALWAYS_ALLOW);
        assertThat(toolPermissionConverter.convertToEntityAttribute("ALLOW")).isEqualTo(ToolPermission.ALWAYS_ALLOW);
        assertThat(toolPermissionConverter.convertToEntityAttribute("NEVER_ASK")).isEqualTo(ToolPermission.ALWAYS_ALLOW);
        assertThat(toolPermissionConverter.convertToEntityAttribute("ALWAYS_ALLOW")).isEqualTo(ToolPermission.ALWAYS_ALLOW);
        assertThat(toolPermissionConverter.convertToEntityAttribute("UNKNOWN")).isEqualTo(ToolPermission.DENY);
    }
}
