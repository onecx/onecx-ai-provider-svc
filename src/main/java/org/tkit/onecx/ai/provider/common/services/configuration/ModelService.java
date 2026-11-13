package org.tkit.onecx.ai.provider.common.services.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.domain.daos.ModelDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

@ApplicationScoped
public class ModelService {

    private static final int MAX_CONFIG_LENGTH = 255;

    @Inject
    ModelDAO dao;

    @Inject
    ProviderDAO providerDAO;

    @Inject
    OpenAiModelConfigValidator modelConfigValidator;

    public Model create(Model model) {
        prepare(model, true);
        return dao.create(model);
    }

    public Model update(Model model, boolean configChanged) {
        prepare(model, configChanged);
        return dao.update(model);
    }

    private void prepare(Model model, boolean validateConfig) {
        var provider = model.getProvider();
        if (provider != null && provider.getId() != null) {
            var persistedProvider = providerDAO.findById(provider.getId());
            if (persistedProvider != null) {
                provider = persistedProvider;
                model.setProvider(provider);
            }
        }
        var config = model.getModelConfig();
        if (!validateConfig || config == null) {
            return;
        }
        if (config.length() > MAX_CONFIG_LENGTH) {
            throw new ModelConfigValidationException("modelConfig must not exceed 255 characters");
        }
        if (provider != null && provider.getType() == ProviderType.OPENAI) {
            modelConfigValidator.validate(config);
        }
    }
}
