package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.SkillSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.GlobalSkill;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class GlobalSkillDAO extends AbstractDAO<GlobalSkill> {

    public PageResult<GlobalSkill> findGlobalSkillsByCriteria(SkillSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(GlobalSkill.class);
            var root = cq.from(GlobalSkill.class);
            List<Predicate> predicates = new ArrayList<>();

            addSearchStringPredicate(predicates, cb, root.get("name"), criteria.getName());

            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));

            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_GLOBAL_SKILLS_BY_CRITERIA, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_GLOBAL_SKILLS_BY_CRITERIA,
    }
}
