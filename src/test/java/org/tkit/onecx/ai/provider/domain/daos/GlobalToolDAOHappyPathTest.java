package org.tkit.onecx.ai.provider.domain.daos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.criteria.ToolSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.test.WithDBData;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
class GlobalToolDAOHappyPathTest extends AbstractTest {

    @Inject
    GlobalToolDAO dao;

    @Test
    void findGlobalToolsByCriteria_withTypeAndTextFilters_returnsExpectedTool() {
        var criteria = new ToolSearchCriteria();
        criteria.setName("global-tool2");
        criteria.setType(ToolType.MCP);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        var result = dao.findGlobalToolsByCriteria(criteria);

        assertThat(result).isNotNull();
        assertThat(result.getStream()).singleElement().satisfies(tool -> {
            assertThat(tool.getId()).isEqualTo("gtool-22-222");
            assertThat(tool.getType()).isEqualTo(ToolType.MCP);
        });
    }
}
