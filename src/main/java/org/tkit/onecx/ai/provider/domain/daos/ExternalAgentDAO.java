package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.ExternalAgentSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class ExternalAgentDAO extends AbstractDAO<ExternalAgent> {
    public PageResult<ExternalAgent> findExternalAgentsByCriteria(ExternalAgentSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(ExternalAgent.class);
            var root = cq.from(ExternalAgent.class);
            List<Predicate> predicates = new ArrayList<>();
            addSearchStringPredicate(predicates, cb, root.get("name"), criteria.getName());
            addSearchStringPredicate(predicates, cb, root.get("description"), criteria.getDescription());
            if (criteria.getEnabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), criteria.getEnabled()));
            }
            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));
            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_EXTERNAL_AGENTS_BY_CRITERIA, ex);
        }
    }

    public List<ExternalAgent> findExternalAgentsByGroupId(String groupId) {
        try {
            if (groupId == null || groupId.isBlank()) {
                return List.of();
            }
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(ExternalAgent.class);
            var root = cq.from(ExternalAgent.class);
            var groupJoin = root.join("groups");
            cq.select(root).distinct(true)
                    .where(cb.equal(groupJoin.get("id"), groupId))
                    .orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_EXTERNAL_AGENTS_BY_GROUP_ID, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_EXTERNAL_AGENTS_BY_CRITERIA,
        ERROR_FIND_EXTERNAL_AGENTS_BY_GROUP_ID
    }
}
