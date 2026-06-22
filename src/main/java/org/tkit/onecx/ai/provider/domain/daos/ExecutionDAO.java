package org.tkit.onecx.ai.provider.domain.daos;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.tkit.onecx.ai.provider.domain.models.Execution;
import org.tkit.quarkus.jpa.daos.AbstractDAO;

/**
 * DAO for Execution entity.
 *
 * Provides persistence operations for execution lifecycle tracking and state management.
 */
@ApplicationScoped
public class ExecutionDAO extends AbstractDAO<Execution> {

    @PersistenceContext
    EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /**
     * Finds an execution by execution ID.
     *
     * @param executionId the unique execution ID
     * @return the execution if found, or null
     */
    public Execution findByExecutionId(String executionId) {
        if (executionId == null || executionId.isBlank()) {
            return null;
        }

        return em.createNamedQuery("Execution.findByExecutionId", Execution.class)
                .setParameter("executionId", executionId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Counts executions for a specific agent.
     *
     * @param agentId the agent ID
     * @param tenantId the tenant ID
     * @return count of executions
     */
    public long countByAgentId(String agentId, String tenantId) {
        if (agentId == null || tenantId == null) {
            return 0;
        }

        return em.createQuery("SELECT COUNT(e) FROM Execution e " +
                "WHERE e.agentIdSnapshot = :agentId AND e.tenantId = :tenantId", Long.class)
                .setParameter("agentId", agentId)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
    }
}
