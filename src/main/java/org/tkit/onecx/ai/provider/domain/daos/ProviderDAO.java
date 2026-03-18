package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.ProviderSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.Provider_;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class ProviderDAO extends AbstractDAO<Provider> {

    public Provider findByKey(String key) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Provider.class);
            var root = cq.from(Provider.class);
            cq.where(cb.equal(root.get(Provider_.key), key));

            return this.getEntityManager().createQuery(cq).getSingleResult();

        } catch (NoResultException nre) {
            return null;
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_PROVIDER_BY_KEY, ex);
        }
    }

    public PageResult<Provider> findByCriteria(ProviderSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Provider.class);
            var root = cq.from(Provider.class);
            List<Predicate> predicates = new ArrayList<>();
            addSearchStringPredicate(predicates, cb, root.get(Provider_.NAME), criteria.getName());
            addSearchStringPredicate(predicates, cb, root.get(Provider_.DESCRIPTION), criteria.getDescription());
            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));
            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_PROVIDERS_BY_CRITERIA, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_PROVIDERS_BY_CRITERIA,
        ERROR_FIND_PROVIDER_BY_KEY
    }
}
