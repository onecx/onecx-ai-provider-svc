package org.tkit.onecx.ai.provider.domain.models;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "GLOBAL_SCAFFOLD")
public class GlobalScaffold extends AbstractScaffold {

    @ManyToMany
    @JoinTable(name = "GLOBAL_SCAFFOLD_GLOBAL_SKILL_RL", joinColumns = @JoinColumn(name = "GLOBAL_SCAFFOLD_ID"), inverseJoinColumns = @JoinColumn(name = "GLOBAL_SKILL_ID"))
    private Set<GlobalSkill> skills;
}
