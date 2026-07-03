package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractScaffold extends TraceableEntity {

    @Column(name = "NAME")
    private String name;

    @Column(name = "SYSTEM_PROMPT")
    private String systemPrompt;

    @Column(name = "SOURCE_PRODUCT")
    private String sourceProduct;

}
