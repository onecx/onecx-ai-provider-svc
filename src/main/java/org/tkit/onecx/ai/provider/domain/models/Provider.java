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
@Table(name = "PROVIDER", uniqueConstraints = {
        @UniqueConstraint(name = "PROVIDER_KEY", columnNames = { "KEY", "TENANT_ID" })
})
@SuppressWarnings("squid:S2160")
public class Provider extends TraceableEntity {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "KEY")
    private String key;

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
