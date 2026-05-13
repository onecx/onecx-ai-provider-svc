package org.tkit.onecx.ai.provider.domain.daos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.quarkus.test.WithDBData;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
class ConfigurationDAOQueryTest {

    @Inject
    ConfigurationDAO dao;

    @Test
    void findAllConfigurationsByFilterKey_coversNullAndNonNullBranches() {
        var withoutFilterKey = dao.findAllConfigurationsByFilterKey(null);
        var withAppIdFilterKey = dao.findAllConfigurationsByFilterKey("APP_ID");

        assertThat(withoutFilterKey).hasSize(2);
        assertThat(withAppIdFilterKey).hasSize(1);
    }
}
