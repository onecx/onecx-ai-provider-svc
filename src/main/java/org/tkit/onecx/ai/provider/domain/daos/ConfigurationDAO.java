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

    public PageResult<Configuration> findAIConfigurationsByCriteria(ConfigurationSearchCriteria criteria) {
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

    public List<Configuration> findAllConfigurationsByFilterKey(String filterKey) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Configuration.class);
            var root = cq.from(Configuration.class);

            if (filterKey == null) {
                cq.where(cb.isNull(root.get(Configuration_.filter)));
            } else {
                cq.where(cb.equal(root.get(Configuration_.filter).get("key"), filterKey));
            }

            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_CONFIGURATIONS_BY_CRITERIA,
        ERROR_FIND_CONFIGURATIONS_BY_FILTER_KEY
    }
}
