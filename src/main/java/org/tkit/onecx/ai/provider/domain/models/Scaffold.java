package org.tkit.onecx.ai.provider.domain.models;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.TenantId;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SCAFFOLD")
public class Scaffold extends AbstractScaffold {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Transient
    private String source = "TENANT";

    @ManyToMany
    @JoinTable(name = "SCAFFOLD_SKILL_RL", joinColumns = @JoinColumn(name = "SCAFFOLD_ID"), inverseJoinColumns = @JoinColumn(name = "SKILL_ID"))
    private Set<Skill> skills;

    @ManyToMany
    @JoinTable(name = "SCAFFOLD_GLOBAL_SKILL_RL", joinColumns = @JoinColumn(name = "SCAFFOLD_ID"), inverseJoinColumns = @JoinColumn(name = "GLOBAL_SKILL_ID"))
    private Set<GlobalSkill> globalSkills;
}
