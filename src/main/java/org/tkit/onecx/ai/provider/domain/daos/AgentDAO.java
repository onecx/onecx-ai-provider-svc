package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.AgentSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class AgentDAO extends AbstractDAO<Agent> {

    public PageResult<Agent> findAgentsByCriteria(AgentSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Agent.class);
            var root = cq.from(Agent.class);

            List<Predicate> predicates = new ArrayList<>();
            addSearchStringPredicate(predicates, cb, root.get("name"), criteria.getName());
            addSearchStringPredicate(predicates, cb, root.get("description"), criteria.getDescription());
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));

            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_AGENTS_BY_CRITERIA, ex);
        }
    }

    public List<Agent> findAllAgentsByFilterKey(String filterKey) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Agent.class);
            var root = cq.from(Agent.class);

            if (filterKey == null) {
                cq.where(cb.isNull(root.get("filter")));
            } else {
                cq.where(cb.equal(root.get("filter").get("key"), filterKey));
            }

            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_AGENTS_BY_FILTER_KEY, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_AGENTS_BY_CRITERIA,
        ERROR_FIND_AGENTS_BY_FILTER_KEY
    }
}
