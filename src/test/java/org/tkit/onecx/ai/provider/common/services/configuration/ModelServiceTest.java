package org.tkit.onecx.ai.provider.common.services.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tkit.onecx.ai.provider.domain.daos.ModelDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ModelServiceTest extends AbstractTest {

    @Inject
    ModelService service;

    @InjectMock
    ModelDAO dao;

    @InjectMock
    ProviderDAO providerDAO;

    @ParameterizedTest
    @ValueSource(strings = { "missing", "no-id", "no-provider" })
    void createPreservesUnresolvedProviderAndOpaqueConfigAtLengthLimit(String providerReference) {
        var model = new Model();
        var provider = new Provider();
        provider.setId("no-id".equals(providerReference) ? null : providerReference);
        if (!"no-provider".equals(providerReference)) {
            model.setProvider(provider);
        }
        var originalProvider = model.getProvider();
        model.setModelConfig("x".repeat(255));
        when(dao.create(model)).thenReturn(model);

        var created = service.create(model);

        assertThat(created.getProvider()).isSameAs(originalProvider);
        assertThat(created.getModelConfig()).hasSize(255);
    }

    @Test
    void createUsesPersistedProviderTypeWithOneLookup() {
        var requestProvider = new Provider();
        requestProvider.setId("openai");
        requestProvider.setType(ProviderType.ANTHROPIC);
        var persistedProvider = new Provider();
        persistedProvider.setId("openai");
        persistedProvider.setType(ProviderType.OPENAI);
        var model = new Model();
        model.setProvider(requestProvider);
        model.setModelConfig("{\"api\":\"responses\"}");
        when(providerDAO.findById("openai")).thenReturn(persistedProvider);
        when(dao.create(model)).thenReturn(model);

        var created = service.create(model);

        assertThat(created.getProvider()).isSameAs(persistedProvider);
        assertThat(created.getModelConfig()).isEqualTo("{\"api\":\"responses\"}");
        verify(providerDAO).findById("openai");
        verifyNoMoreInteractions(providerDAO);
    }

    @Test
    void createRejectsInvalidConfigDespiteSpoofedProviderType() {
        var requestProvider = new Provider();
        requestProvider.setId("openai");
        requestProvider.setType(ProviderType.ANTHROPIC);
        var persistedProvider = new Provider();
        persistedProvider.setType(ProviderType.OPENAI);
        var model = new Model();
        model.setProvider(requestProvider);
        model.setModelConfig("opaque");
        when(providerDAO.findById("openai")).thenReturn(persistedProvider);

        assertThatThrownBy(() -> service.create(model))
                .isInstanceOf(ModelConfigValidationException.class)
                .hasMessage("modelConfig must contain valid JSON");
        verify(dao, never()).create(model);
    }
}
