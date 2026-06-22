package org.tkit.onecx.ai.provider.domain.daos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.quarkus.test.WithDBData;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
class AgentDAOQueryTest {

    @Inject
    AgentDAO dao;

    @Test
    void findAllAgentsByFilterKey_coversNullAndNonNullBranches() {
        assertThat(dao.findAllAgentsByFilterKey(null)).hasSize(1);
        assertThat(dao.findAllAgentsByFilterKey("APP_ID")).hasSize(1);
    }
}
