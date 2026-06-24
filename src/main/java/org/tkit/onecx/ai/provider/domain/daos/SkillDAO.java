package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.SkillSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.GlobalSkill;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class SkillDAO extends AbstractDAO<Skill> {

    @Inject
    GlobalSkillDAO globalSkillDAO;

    public PageResult<Skill> findSkillsByCriteria(SkillSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Skill.class);
            var root = cq.from(Skill.class);
            List<Predicate> predicates = new ArrayList<>();

            addSearchStringPredicate(predicates, cb, root.get("name"), criteria.getName());

            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));

            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_SKILLS_BY_CRITERIA, ex);
        }
    }

    public PageResult<Skill> findSkillsByCriteriaIncludingGlobal(SkillSearchCriteria criteria) {
        try {
            var tenantSkills = findSkillsByCriteria(criteria);
            var globalSkills = globalSkillDAO.findGlobalSkillsByCriteria(criteria);

            List<Skill> combined = new ArrayList<>();
            combined.addAll(tenantSkills.getStream().collect(Collectors.toList()));
            combined.addAll(globalSkills.getStream().map(this::fromGlobal).collect(Collectors.toList()));

            combined.sort(
                    Comparator.comparing(Skill::getCreationDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

            int pageNumber = criteria.getPageNumber();
            int pageSize = criteria.getPageSize();
            int startIndex = pageNumber * pageSize;

            List<Skill> pageContent = combined.stream()
                    .skip(startIndex)
                    .limit(pageSize)
                    .collect(Collectors.toList());

            return new PageResult<>(combined.size(), pageContent.stream(), pageNumber, pageSize);
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_SKILLS_BY_CRITERIA_INCLUDING_GLOBAL, ex);
        }
    }

    private Skill fromGlobal(GlobalSkill globalSkill) {
        Skill skill = new Skill();
        skill.setId(globalSkill.getId());
        skill.setCreationDate(globalSkill.getCreationDate());
        skill.setCreationUser(globalSkill.getCreationUser());
        skill.setModificationDate(globalSkill.getModificationDate());
        skill.setModificationUser(globalSkill.getModificationUser());
        skill.setModificationCount(globalSkill.getModificationCount());
        skill.setName(globalSkill.getName());
        skill.setDescription(globalSkill.getDescription());
        skill.setInstruction(globalSkill.getInstruction());
        return skill;
    }

    public enum ErrorKeys {
        ERROR_FIND_SKILLS_BY_CRITERIA,
        ERROR_FIND_SKILLS_BY_CRITERIA_INCLUDING_GLOBAL,
    }
}
