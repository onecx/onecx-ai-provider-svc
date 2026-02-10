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
@NamedEntityGraph(name = Configuration.AI_CONFIGURATION_LOAD, includeAllAttributes = true)
public class Configuration extends TraceableEntity {

    public static final String AI_CONFIGURATION_LOAD = "AI_CONFIGURATION_LOAD";

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LLM_SYSTEM_MESSAGE")
    private String llmSystemMessage;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "PROVIDER_ID")
    private Provider provider;

    @Embedded
    private Filter filter;

    @ManyToMany
    @JoinTable(name = "CONFIGURATION_MCP_SERVER", joinColumns = @JoinColumn(name = "CONFIGURATION_ID"), inverseJoinColumns = @JoinColumn(name = "MCP_SERVER_ID"))
    private Set<MCPServer> mcpServers;

}
