package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SKILL")
public class Skill extends TraceableEntity {

    @Column(name = "NAME")
    private String name;

    @Column(name = "INPUT_SCHEMA")
    private String inputSchema;

    @Column(name = "OUTPUT_SCHEMA")
    private String outputSchema;
}
