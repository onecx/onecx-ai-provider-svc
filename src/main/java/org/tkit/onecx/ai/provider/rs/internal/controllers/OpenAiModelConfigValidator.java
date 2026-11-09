package org.tkit.onecx.ai.provider.rs.internal.controllers;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class OpenAiModelConfigValidator {

    private static final int MAX_CONFIG_LENGTH = 255;
    private static final Set<String> ALLOWED_KEYS = Set.of("api", "reasoningEffort");
    private static final Set<String> ALLOWED_APIS = Set.of("chat-completions", "responses");

    @Inject
    ObjectMapper objectMapper;

    public void validate(String modelConfig) {
        if (modelConfig.length() > MAX_CONFIG_LENGTH) {
            throw new ModelConfigValidationException("modelConfig must not exceed 255 characters");
        }
        if (modelConfig.isBlank()) {
            return;
        }

        JsonNode config;
        try {
            config = objectMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(modelConfig);
        } catch (JsonProcessingException e) {
            throw new ModelConfigValidationException("modelConfig must contain valid JSON");
        }
        if (config == null || !config.isObject()) {
            throw new ModelConfigValidationException("modelConfig must be a JSON object");
        }

        config.fieldNames().forEachRemaining(key -> {
            if (!ALLOWED_KEYS.contains(key)) {
                throw new ModelConfigValidationException("modelConfig contains unsupported key: " + key);
            }
        });

        JsonNode api = config.get("api");
        if (api != null && (!api.isTextual() || !ALLOWED_APIS.contains(api.textValue()))) {
            throw new ModelConfigValidationException("modelConfig.api must be chat-completions or responses");
        }

        JsonNode reasoningEffort = config.get("reasoningEffort");
        if (reasoningEffort != null && !reasoningEffort.isTextual()) {
            throw new ModelConfigValidationException("modelConfig.reasoningEffort must be a string");
        }
    }
}
