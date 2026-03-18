package org.tkit.onecx.ai.provider.domain.models;

import java.util.Set;

import jakarta.persistence.*;

import org.hibernate.annotations.TenantId;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "CONFIGURATION")
public class Configuration extends TraceableEntity {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LLM_SYSTEM_MESSAGE")
    private String llmSystemMessage;

    @Column(name = "PROVIDER_KEY")
    private String llmProvider;

    @Embedded
    private Filter filter;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "MCP_SERVERS", joinColumns = @JoinColumn(name = "CONFIGURATION_ID"))
    @Column(name = "MCP_SERVER_KEYS", nullable = false)
    private Set<String> mcpServers;

}
