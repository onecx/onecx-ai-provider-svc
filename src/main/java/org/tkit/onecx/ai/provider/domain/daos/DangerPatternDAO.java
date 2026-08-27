package org.tkit.onecx.ai.provider.domain.daos;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.onecx.ai.provider.domain.models.DangerPattern;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.exceptions.DAOException;

@ApplicationScoped
public class DangerPatternDAO extends AbstractDAO<DangerPattern> {

    public List<DangerPattern> findAllPatterns() {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(DangerPattern.class);
            cq.from(DangerPattern.class);
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_ALL_PATTERNS, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_ALL_PATTERNS,
    }
}
