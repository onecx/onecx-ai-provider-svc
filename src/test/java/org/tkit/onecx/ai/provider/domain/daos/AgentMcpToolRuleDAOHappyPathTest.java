package org.tkit.onecx.ai.provider.domain.daos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.test.WithDBData;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
class AgentMcpToolRuleDAOHappyPathTest extends AbstractTest {

    @Inject
    AgentMcpToolRuleDAO dao;

    @Test
    void findByAgentId_returnsRulesForAgent() {
        var rules = dao.findByAgentId("agent-11-111");
        assertThat(rules).hasSize(2);
        assertThat(rules).extracting(r -> r.getToolName()).contains("getProposal", "deleteProposal");
    }

    @Test
    void findByAgentId_returnsEmptyForUnknownAgent() {
        assertThat(dao.findByAgentId("agent-none")).isEmpty();
    }

    @Test
    void deleteByAgentId_removesAllRulesForAgent() {
        assertThat(dao.findByAgentId("agent-11-111")).hasSize(2);
        dao.deleteByAgentId("agent-11-111");
        assertThat(dao.findByAgentId("agent-11-111")).isEmpty();
    }

    @Test
    void deleteByAgentAndToolId_removesRulesForAgentAndTool() {
        assertThat(dao.findByAgentAndToolId("agent-11-111", "tool-11-111")).hasSize(2);
        dao.deleteByAgentAndToolId("agent-11-111", "tool-11-111");
        assertThat(dao.findByAgentAndToolId("agent-11-111", "tool-11-111")).isEmpty();
    }
}
