package org.tkit.onecx.ai.provider.domain.daos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.criteria.ToolSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.quarkus.test.WithDBData;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
class ToolDAOQueryTest {

    @Inject
    ToolDAO dao;

    @Test
    void findToolsByCriteria_filtersByType() {
        var criteria = new ToolSearchCriteria();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        assertThat(dao.findToolsByCriteria(criteria).getStream()).hasSize(2);

        criteria.setType(ToolType.MCP);
        assertThat(dao.findToolsByCriteria(criteria).getStream()).hasSize(1);
    }
}
