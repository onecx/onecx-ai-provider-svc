package org.tkit.onecx.ai.provider.domain.daos;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.tkit.onecx.ai.provider.domain.models.AgentConfigurationVersion;

@ApplicationScoped
public class AgentConfigurationVersionDAO {

    private static final String PARAM_AGENT_ID = "agentId";

    @Inject
    EntityManager entityManager;

    public void create(AgentConfigurationVersion version) {
        entityManager.persist(version);
        entityManager.flush();
    }

    public boolean existsByAgentAndName(String agentId, String name) {
        return entityManager.createQuery(
                "select count(v) from AgentConfigurationVersion v where v.agentId = :agentId and v.name = :name",
                Long.class)
                .setParameter(PARAM_AGENT_ID, agentId)
                .setParameter("name", name)
                .getSingleResult() > 0;
    }

    public AgentConfigurationVersion findByAgentAndId(String agentId, String versionId) {
        return entityManager.createQuery(
                "select v from AgentConfigurationVersion v where v.agentId = :agentId and v.id = :versionId",
                AgentConfigurationVersion.class)
                .setParameter(PARAM_AGENT_ID, agentId)
                .setParameter("versionId", versionId)
                .getResultStream().findFirst().orElse(null);
    }

    public List<AgentConfigurationVersion> findAllByAgent(String agentId) {
        return entityManager.createQuery(
                "select v from AgentConfigurationVersion v where v.agentId = :agentId order by v.name",
                AgentConfigurationVersion.class)
                .setParameter(PARAM_AGENT_ID, agentId)
                .getResultList();
    }
}