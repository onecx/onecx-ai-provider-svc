package org.tkit.onecx.ai.provider.domain.models;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;
import org.tkit.onecx.ai.provider.domain.models.enums.AuthMode;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "EXTERNAL_AGENT")
public class ExternalAgent extends TraceableEntity {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DISCOVERY_URL")
    private String discoveryUrl;

    @Column(name = "API_KEY")
    private String apiKey;

    @Column(name = "AUTH_MODE")
    @Enumerated(EnumType.STRING)
    private AuthMode authMode;

    @Column(name = "ENABLED")
    private Boolean enabled;

    @ManyToMany
    @JoinTable(name = "EXTERNAL_AGENT_GROUP_RL", joinColumns = @JoinColumn(name = "EXTERNAL_AGENT_ID"), inverseJoinColumns = @JoinColumn(name = "GROUP_ID"))
    private Set<AgentGroup> groups;
}
