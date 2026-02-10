package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.*;

import org.hibernate.annotations.TenantId;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "PROVIDER")
@NamedEntityGraph(name = Provider.AI_PROVIDER_LOAD, includeAllAttributes = true)
public class Provider extends TraceableEntity {

    public static final String AI_PROVIDER_LOAD = "AI_PROVIDER_LOAD";

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "TYPE")
    @Enumerated(EnumType.STRING)
    private ProviderType type;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LLM_URL")
    private String llmUrl;

    @Column(name = "MODEL_NAME")
    private String modelName;

    @Column(name = "API_KEY")
    private String apiKey;

}
