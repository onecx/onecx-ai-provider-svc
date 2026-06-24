package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.ToolSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class GlobalToolDAO extends AbstractDAO<GlobalTool> {

    public PageResult<GlobalTool> findGlobalToolsByCriteria(ToolSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(GlobalTool.class);
            var root = cq.from(GlobalTool.class);
            List<Predicate> predicates = new ArrayList<>();

            addSearchStringPredicate(predicates, cb, root.get("name"), criteria.getName());
            addSearchStringPredicate(predicates, cb, root.get("description"), criteria.getDescription());
            addSearchStringPredicate(predicates, cb, root.get("url"), criteria.getUrl());
            if (criteria.getType() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.getType()));
            }

            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));

            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_GLOBAL_TOOLS_BY_CRITERIA, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_GLOBAL_TOOLS_BY_CRITERIA,
    }
}
