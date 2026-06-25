package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupOrchestrationMode;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupResponseStrategy;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "AGENT_GROUP")
public class AgentGroup extends TraceableEntity {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ROUTING_INSTRUCTIONS")
    private String routingInstructions;

    @Column(name = "ORCHESTRATION_MODE")
    @Enumerated(EnumType.STRING)
    private AgentGroupOrchestrationMode orchestrationMode = AgentGroupOrchestrationMode.SUPERVISOR_ROUTED;

    @Column(name = "RESPONSE_STRATEGY")
    @Enumerated(EnumType.STRING)
    private AgentGroupResponseStrategy responseStrategy = AgentGroupResponseStrategy.LAST;
}
