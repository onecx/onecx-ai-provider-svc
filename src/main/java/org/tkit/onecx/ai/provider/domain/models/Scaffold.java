package org.tkit.onecx.ai.provider.domain.models;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SCAFFOLD")
public class Scaffold extends TraceableEntity {

    @Column(name = "NAME")
    private String name;

    @Column(name = "SYSTEM_PROMPT")
    private String systemPrompt;

    @Column(name = "SOURCE_PRODUCT")
    private String sourceProduct;

    @ManyToMany
    @JoinTable(name = "SCAFFOLD_SKILL_RL", joinColumns = @JoinColumn(name = "SCAFFOLD_ID"), inverseJoinColumns = @JoinColumn(name = "SKILL_ID"))
    private Set<Skill> skills;
}
