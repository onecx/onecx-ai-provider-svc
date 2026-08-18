package org.tkit.onecx.ai.provider.domain.daos;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.exceptions.DAOException;

@ApplicationScoped
public class AgentMcpToolRuleDAO extends AbstractDAO<AgentMcpToolRule> {

    public List<AgentMcpToolRule> findByAgentId(String agentId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(AgentMcpToolRule.class);
            var root = cq.from(AgentMcpToolRule.class);
            cq.where(cb.equal(root.get("agent").get("id"), agentId));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_AGENT_ID, ex);
        }
    }

    public List<AgentMcpToolRule> findByAgentAndToolId(String agentId, String toolId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(AgentMcpToolRule.class);
            var root = cq.from(AgentMcpToolRule.class);
            cq.where(cb.and(
                    cb.equal(root.get("agent").get("id"), agentId),
                    cb.equal(root.get("tool").get("id"), toolId)));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_AGENT_AND_TOOL_ID, ex);
        }
    }

    public List<AgentMcpToolRule> findByAgentAndToolIds(String agentId, List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(AgentMcpToolRule.class);
            var root = cq.from(AgentMcpToolRule.class);
            cq.where(cb.and(
                    cb.equal(root.get("agent").get("id"), agentId),
                    root.get("tool").get("id").in(toolIds)));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_AGENT_AND_TOOL_IDS, ex);
        }
    }

    public List<AgentMcpToolRule> findByAgentAndGlobalToolIds(String agentId, List<String> globalToolIds) {
        if (globalToolIds == null || globalToolIds.isEmpty()) {
            return List.of();
        }
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(AgentMcpToolRule.class);
            var root = cq.from(AgentMcpToolRule.class);
            cq.where(cb.and(
                    cb.equal(root.get("agent").get("id"), agentId),
                    root.get("globalTool").get("id").in(globalToolIds)));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_AGENT_AND_GLOBAL_TOOL_IDS, ex);
        }
    }

    @Transactional
    public void deleteByAgentId(String agentId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cd = cb.createCriteriaDelete(AgentMcpToolRule.class);
            var root = cd.from(AgentMcpToolRule.class);
            cd.where(cb.equal(root.get("agent").get("id"), agentId));
            this.getEntityManager().createQuery(cd).executeUpdate();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_DELETE_RULES_BY_AGENT_ID, ex);
        }
    }

    @Transactional
    public void deleteByAgentAndToolId(String agentId, String toolId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cd = cb.createCriteriaDelete(AgentMcpToolRule.class);
            var root = cd.from(AgentMcpToolRule.class);
            cd.where(cb.and(
                    cb.equal(root.get("agent").get("id"), agentId),
                    cb.equal(root.get("tool").get("id"), toolId)));
            this.getEntityManager().createQuery(cd).executeUpdate();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_DELETE_RULES_BY_AGENT_AND_TOOL_ID, ex);
        }
    }

    @Transactional
    public void deleteByToolId(String toolId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cd = cb.createCriteriaDelete(AgentMcpToolRule.class);
            var root = cd.from(AgentMcpToolRule.class);
            cd.where(cb.equal(root.get("tool").get("id"), toolId));
            this.getEntityManager().createQuery(cd).executeUpdate();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_DELETE_RULES_BY_TOOL_ID, ex);
        }
    }

    @Transactional
    public void deleteByGlobalToolId(String globalToolId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cd = cb.createCriteriaDelete(AgentMcpToolRule.class);
            var root = cd.from(AgentMcpToolRule.class);
            cd.where(cb.equal(root.get("globalTool").get("id"), globalToolId));
            this.getEntityManager().createQuery(cd).executeUpdate();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_DELETE_RULES_BY_GLOBAL_TOOL_ID, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_RULES_BY_AGENT_ID,
        ERROR_FIND_RULES_BY_AGENT_AND_TOOL_ID,
        ERROR_FIND_RULES_BY_AGENT_AND_TOOL_IDS,
        ERROR_FIND_RULES_BY_AGENT_AND_GLOBAL_TOOL_IDS,
        ERROR_DELETE_RULES_BY_AGENT_ID,
        ERROR_DELETE_RULES_BY_AGENT_AND_TOOL_ID,
        ERROR_DELETE_RULES_BY_TOOL_ID,
        ERROR_DELETE_RULES_BY_GLOBAL_TOOL_ID,
    }
}
