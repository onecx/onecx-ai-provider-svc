package org.tkit.onecx.ai.provider.common.services.agent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.daos.ScaffoldDAO;
import org.tkit.onecx.ai.provider.domain.daos.SkillDAO;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.onecx.ai.provider.rs.internal.mappers.SkillMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.SkillDTO;

@ApplicationScoped
public class ScaffoldService {

    @Inject
    ScaffoldDAO scaffoldDAO;

    @Inject
    SkillDAO skillDAO;

    @Inject
    SkillMapper skillMapper;

    @Transactional
    public Scaffold updateScaffold(Scaffold updatedScaffold, List<SkillDTO> skills) {
        // Update the scaffold

        // Update the skills associated with the scaffold
        if (skills != null && !skills.isEmpty()) {
            Set<Skill> skillsToAdd = new HashSet<>();
            skills.forEach(skillDTO -> {
                if (skillDTO.getId() == null) {
                    var newSkill = skillDAO.create(skillMapper.mapCreate(skillDTO));
                    skillsToAdd.add(newSkill);
                } else {
                    // If the skill ID is not null, find the existing skill and associate it with the scaffold
                    var skill = skillDAO.findById(skillDTO.getId());
                    if (skill != null) {
                        skillsToAdd.add(skill);
                    }
                }
            });
            updatedScaffold.setSkills(skillsToAdd);
            updatedScaffold = scaffoldDAO.update(updatedScaffold); // Update the scaffold with the new skills
        }
        return updatedScaffold;
    }

    @Transactional
    public Scaffold createScaffold(Scaffold createdScaffold, List<SkillDTO> skills) {
        if (skills != null && !skills.isEmpty()) {
            Set<Skill> skillsToAdd = new HashSet<>();
            skills.forEach(skillDTO -> {
                if (skillDTO.getId() == null) {
                    var newSkill = skillDAO.create(skillMapper.mapCreate(skillDTO));
                    skillsToAdd.add(newSkill);
                } else {
                    var skill = skillDAO.findById(skillDTO.getId());
                    if (skill != null) {
                        skillsToAdd.add(skill);
                    }
                }
            });
            createdScaffold.setSkills(skillsToAdd);
            createdScaffold = scaffoldDAO.update(createdScaffold);
        }
        return createdScaffold;
    }
}
