package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.criteria.ScaffoldSearchCriteria;
import org.tkit.onecx.ai.provider.domain.mappers.ScaffoldMapper;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class ScaffoldDAO extends AbstractDAO<Scaffold> {

    @Inject
    GlobalScaffoldDAO globalScaffoldDAO;

    @Inject
    ScaffoldMapper scaffoldMapper;

    public PageResult<Scaffold> findScaffoldsByCriteria(ScaffoldSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Scaffold.class);
            var root = cq.from(Scaffold.class);
            List<Predicate> predicates = new ArrayList<>();

            addSearchStringPredicate(predicates, cb, root.get("name"), criteria.getName());
            addSearchStringPredicate(predicates, cb, root.get("sourceProduct"), criteria.getSourceProduct());

            if (!predicates.isEmpty()) {
                cq.where(predicates.toArray(new Predicate[] {}));
            }
            cq.orderBy(cb.desc(root.get(AbstractTraceableEntity_.CREATION_DATE)));

            return createPageQuery(cq, Page.of(criteria.getPageNumber(), criteria.getPageSize())).getPageResult();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_SCAFFOLDS_BY_CRITERIA, ex);
        }
    }

    @Transactional
    public PageResult<Scaffold> findScaffoldsByCriteriaIncludingGlobal(ScaffoldSearchCriteria criteria) {
        try {
            var tenantScaffolds = findScaffoldsByCriteria(criteria);
            var globalScaffolds = globalScaffoldDAO.findGlobalScaffoldsByCriteria(criteria);

            List<Scaffold> combined = new ArrayList<>();
            combined.addAll(tenantScaffolds.getStream().toList());
            combined.addAll(globalScaffolds.getStream().map(scaffoldMapper::fromGlobal).toList());

            combined.sort(Comparator.comparing(Scaffold::getCreationDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed());

            int pageNumber = criteria.getPageNumber();
            int pageSize = criteria.getPageSize();
            int startIndex = pageNumber * pageSize;

            List<Scaffold> pageContent = combined.stream()
                    .skip(startIndex)
                    .limit(pageSize)
                    .toList();

            return new PageResult<>(combined.size(), pageContent.stream(), pageNumber, pageSize);
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_SCAFFOLDS_BY_CRITERIA_INCLUDING_GLOBAL, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_SCAFFOLDS_BY_CRITERIA,
        ERROR_FIND_SCAFFOLDS_BY_CRITERIA_INCLUDING_GLOBAL,
    }
}
