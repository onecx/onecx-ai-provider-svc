package org.tkit.onecx.ai.provider.domain.daos;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.models.McpToolRule;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.exceptions.DAOException;

@ApplicationScoped
public class McpToolRuleDAO extends AbstractDAO<McpToolRule> {

    public List<McpToolRule> findByToolId(String toolId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(McpToolRule.class);
            var root = cq.from(McpToolRule.class);
            cq.where(cb.equal(root.get("tool").get("id"), toolId));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_TOOL_ID, ex);
        }
    }

    public List<McpToolRule> findByGlobalToolId(String globalToolId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(McpToolRule.class);
            var root = cq.from(McpToolRule.class);
            cq.where(cb.equal(root.get("globalTool").get("id"), globalToolId));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_GLOBAL_TOOL_ID, ex);
        }
    }

    public List<McpToolRule> findByToolIds(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(McpToolRule.class);
            var root = cq.from(McpToolRule.class);
            cq.where(root.get("tool").get("id").in(toolIds));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_TOOL_IDS, ex);
        }
    }

    public List<McpToolRule> findByGlobalToolIds(List<String> globalToolIds) {
        if (globalToolIds == null || globalToolIds.isEmpty()) {
            return List.of();
        }
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery(McpToolRule.class);
            var root = cq.from(McpToolRule.class);
            cq.where(root.get("globalTool").get("id").in(globalToolIds));
            return this.getEntityManager().createQuery(cq).getResultList();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_FIND_RULES_BY_GLOBAL_TOOL_IDS, ex);
        }
    }

    @Transactional
    public void deleteByToolId(String toolId) {
        try {
            var cb = this.getEntityManager().getCriteriaBuilder();
            var cd = cb.createCriteriaDelete(McpToolRule.class);
            var root = cd.from(McpToolRule.class);
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
            var cd = cb.createCriteriaDelete(McpToolRule.class);
            var root = cd.from(McpToolRule.class);
            cd.where(cb.equal(root.get("globalTool").get("id"), globalToolId));
            this.getEntityManager().createQuery(cd).executeUpdate();
        } catch (Exception ex) {
            throw new DAOException(ErrorKeys.ERROR_DELETE_RULES_BY_GLOBAL_TOOL_ID, ex);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_RULES_BY_TOOL_ID,
        ERROR_FIND_RULES_BY_GLOBAL_TOOL_ID,
        ERROR_FIND_RULES_BY_TOOL_IDS,
        ERROR_FIND_RULES_BY_GLOBAL_TOOL_IDS,
        ERROR_DELETE_RULES_BY_TOOL_ID,
        ERROR_DELETE_RULES_BY_GLOBAL_TOOL_ID,
    }
}
