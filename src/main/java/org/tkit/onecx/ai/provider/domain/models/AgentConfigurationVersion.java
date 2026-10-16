package org.tkit.onecx.ai.provider.domain.models;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import lombok.Getter;

@Getter
@Entity
@Immutable
@Table(name = "AGENT_CONFIGURATION_VERSION")
public class AgentConfigurationVersion {

    @Id
    @Column(name = "GUID", updatable = false, nullable = false)
    private String id;

    @TenantId
    @Column(name = "TENANT_ID", updatable = false, nullable = false)
    private String tenantId;

    @Column(name = "AGENT_ID", updatable = false, nullable = false)
    private String agentId;

    @Column(name = "NAME", updatable = false, nullable = false)
    private String name;

    @Column(name = "CONFIGURATION", columnDefinition = "text", updatable = false, nullable = false)
    private String configuration;

    protected AgentConfigurationVersion() {
    }

    public AgentConfigurationVersion(Agent agent, String name, String configuration) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = agent.getTenantId();
        this.agentId = agent.getId();
        this.name = name;
        this.configuration = configuration;
    }
}