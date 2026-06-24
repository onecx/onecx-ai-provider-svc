package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.ScaffoldSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.GlobalScaffold;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class GlobalScaffoldDAO extends AbstractDAO<GlobalScaffold> {

    public PageResult<GlobalScaffold> findGlobalScaffoldsByCriteria(ScaffoldSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(GlobalScaffold.class);
            var root = cq.from(GlobalScaffold.class);
            List<Predicate> predicates = new ArrayList<>();

            addSearchStringPredicate(predicates, cb, root.get("name"), criteria.getName());
            addSearchStringPredicate(predicates, cb, root.get("sourceProduct"), criteria.getSourceProduct());

            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));

            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_GLOBAL_SCAFFOLDS_BY_CRITERIA, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_GLOBAL_SCAFFOLDS_BY_CRITERIA,
    }
}
