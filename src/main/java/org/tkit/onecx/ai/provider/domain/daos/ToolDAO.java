package org.tkit.onecx.ai.provider.domain.daos;

import static org.tkit.quarkus.jpa.utils.QueryCriteriaUtil.addSearchStringPredicate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.criteria.ToolSearchCriteria;
import org.tkit.onecx.ai.provider.domain.mappers.ToolMapper;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.jpa.exceptions.DAOException;
import org.tkit.quarkus.jpa.models.AbstractTraceableEntity_;

@ApplicationScoped
public class ToolDAO extends AbstractDAO<Tool> {

    @Inject
    GlobalToolDAO globalToolDAO;

    @Inject
    ToolMapper toolMapper;

    public PageResult<Tool> findToolsByCriteria(ToolSearchCriteria criteria) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(Tool.class);
            var root = cq.from(Tool.class);
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
            throw new DAOException(ErrorKeys.ERROR_FIND_TOOLS_BY_CRITERIA, ex);
        }
    }

    @Transactional
    public PageResult<Tool> findToolsByCriteriaIncludingGlobal(ToolSearchCriteria criteria) {
        try {
            var tenantTools = findToolsByCriteria(criteria);
            var globalTools = globalToolDAO.findGlobalToolsByCriteria(criteria);

            List<Tool> combined = new ArrayList<>();
            combined.addAll(tenantTools.getStream().toList());
            combined.addAll(globalTools.getStream().map(toolMapper::fromGlobal).toList());

            combined.sort(
                    Comparator.comparing(Tool::getCreationDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

            int pageNumber = criteria.getPageNumber();
            int pageSize = criteria.getPageSize();
            int startIndex = pageNumber * pageSize;

            List<Tool> pageContent = combined.stream()
                    .skip(startIndex)
                    .limit(pageSize)
                    .toList();

            return new PageResult<>(combined.size(), pageContent.stream(), pageNumber, pageSize);
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_TOOLS_BY_CRITERIA_INCLUDING_GLOBAL, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_TOOLS_BY_CRITERIA,
        ERROR_FIND_TOOLS_BY_CRITERIA_INCLUDING_GLOBAL,
    }
}
