package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Predicate;

import org.tkit.onecx.ai.provider.domain.criteria.ConfigurationSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Configuration_;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class ConfigurationDAO extends AbstractDAO<Configuration> {

    public Configuration findConfigurationsByFilter(String filterKey, String filterValue) {

        if (filterValue == null) {
            return findByFilterKeyEmptyValue(filterKey).stream().findFirst().orElse(null);
        }

        var configurations = findByFilterKey(filterKey);
        return configurations.stream()
                .filter(c -> filterValue.matches(c.getFilterValue().replace("*", ".*")))
                .max((c1, c2) -> {
                    int len1 = c1.getFilterValue().replace("*", "").length();
                    int len2 = c2.getFilterValue().replace("*", "").length();
                    return Integer.compare(len1, len2);
                })
                .orElse(null);
    }

    public PageResult<Configuration> findByCriteria(ConfigurationSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Configuration.class);
            var root = cq.from(Configuration.class);

            List<Predicate> predicates = new ArrayList<>();
            addSearchStringPredicate(predicates, cb, root.get(Configuration_.NAME), criteria.getName());
            addSearchStringPredicate(predicates, cb, root.get(Configuration_.DESCRIPTION), criteria.getDescription());

            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));

            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_CONFIGURATIONS_BY_CRITERIA, ex);
        }
    }

    public List<Configuration> findByFilterKeyEmptyValue(String filterKey) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Configuration.class);
            var root = cq.from(Configuration.class);

            if (filterKey == null) {
                cq.where(cb.isNull(root.get(Configuration_.FILTER_KEY)), cb.isNull(root.get(Configuration_.FILTER_VALUE)));
            } else {
                cq.where(cb.equal(root.get(Configuration_.FILTER_KEY), filterKey),
                        cb.isNull(root.get(Configuration_.FILTER_VALUE)));
            }

            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY_EMPTY_VALUE, ex);
        }
    }

    public List<Configuration> findByFilterKey(String filterKey) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Configuration.class);
            var root = cq.from(Configuration.class);

            if (filterKey == null) {
                cq.where(cb.isNull(root.get(Configuration_.FILTER_KEY)), cb.isNotNull(root.get(Configuration_.FILTER_VALUE)));
            } else {
                cq.where(cb.equal(root.get(Configuration_.FILTER_KEY), filterKey),
                        cb.isNotNull(root.get(Configuration_.FILTER_VALUE)));
            }

            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY_EMPTY_VALUE,
        ERROR_FIND_CONFIGURATIONS_BY_CRITERIA,
        ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY
    }
}
