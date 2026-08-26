package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.tkit.onecx.ai.provider.domain.models.enums.DangerLevel;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "DANGER_PATTERN")
public class DangerPattern extends TraceableEntity {

    @Column(name = "PATTERN", nullable = false)
    private String pattern;

    @Column(name = "DANGER_LEVEL", nullable = false)
    @Enumerated(EnumType.STRING)
    private DangerLevel dangerLevel;

    @Column(name = "DESCRIPTION", length = 1024)
    private String description;

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
