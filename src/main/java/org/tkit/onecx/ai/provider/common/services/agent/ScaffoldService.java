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
        updatedScaffold.setSkills(resolveSkills(skills));
        updatedScaffold = scaffoldDAO.update(updatedScaffold);
        return updatedScaffold;
    }

    @Transactional
    public Scaffold createScaffold(Scaffold createdScaffold, List<SkillDTO> skills) {
        createdScaffold.setSkills(resolveSkills(skills));
        createdScaffold = scaffoldDAO.create(createdScaffold);
        return createdScaffold;
    }

    Set<Skill> resolveSkills(List<SkillDTO> skills) {
        Set<Skill> skillsToAdd = new HashSet<>();
        if (skills != null && !skills.isEmpty()) {
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
        }
        return skillsToAdd;
    }
}
