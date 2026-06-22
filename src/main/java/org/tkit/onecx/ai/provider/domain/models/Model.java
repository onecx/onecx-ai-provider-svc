package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;
import org.tkit.onecx.ai.provider.domain.models.enums.CommunicationMode;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "MODEL")
public class Model extends TraceableEntity {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @ManyToOne
    @JoinColumn(name = "PROVIDER_ID")
    private Provider provider;

    @Column(name = "NAME")
    private String name;

    @Column(name = "MODEL_IDENTIFIER")
    private String modelIdentifier;

    @Column(name = "MODEL_CONFIG")
    private String modelConfig;

    @Column(name = "COMMUNICATION_MODE")
    @Enumerated(EnumType.STRING)
    private CommunicationMode communicationMode;
}
