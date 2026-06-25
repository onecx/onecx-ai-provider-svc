package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class OllamaProviderAdapter implements ProviderAdapter {

    private static final String HEALTH_CHECK_PROMPT = "ping";
    private static final String HEALTH_CHECK_MODEL = "llama2";

    @Inject
    DispatchConfig dispatchConfig;

    @Override
    public boolean supports(ProviderType type) {
        return ProviderType.OLLAMA.equals(type);
    }

    @Override
    public ChatModel createChatModel(Agent agent) {
        if (agent == null || agent.getModel() == null || agent.getModel().getProvider() == null) {
            throw new IllegalArgumentException("Agent has no associated model or provider");
        }
        Provider provider = agent.getModel().getProvider();
        String modelName = agent.getModel().getModelIdentifier();
        if (isBlank(provider.getLlmUrl())) {
            throw new IllegalArgumentException("Ollama provider has no LLM URL configured");
        }
        if (isBlank(modelName)) {
            throw new IllegalArgumentException("Agent model has no model identifier configured");
        }
        return buildModel(provider, modelName);
    }

    @Override
    public String healthCheck(Provider provider) {
        if (provider == null || isBlank(provider.getLlmUrl())) {
            log.warn("Ollama provider configuration incomplete for health check");
            return UNHEALTHY;
        }
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(new UserMessage(HEALTH_CHECK_PROMPT)))
                    .build();
            var response = buildModel(provider, HEALTH_CHECK_MODEL).chat(request);
            if (response == null || response.aiMessage() == null) {
                log.warn("Ollama health check failed for provider '{}' at '{}'", provider.getName(), provider.getLlmUrl());
                return UNHEALTHY;
            }
            return HEALTHY;
        } catch (Exception e) {
            log.warn("Ollama health check failed for provider '{}' at '{}': {}", provider.getName(), provider.getLlmUrl(),
                    e.getMessage());
            return UNHEALTHY;
        }
    }

    private OllamaChatModel buildModel(Provider provider, String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(provider.getLlmUrl())
                .modelName(modelName)
                .customHeaders(createCustomHeaders(provider))
                .timeout(Duration.ofSeconds(dispatchConfig.providerConfig().timeout()))
                .logRequests(dispatchConfig.providerConfig().logRequests())
                .logResponses(dispatchConfig.providerConfig().logResponse())
                .httpClientBuilder(new JaxRsHttpClientBuilderFactory().create())
                .build();
    }

    private Map<String, String> createCustomHeaders(Provider provider) {
        Map<String, String> customHeaders = new HashMap<>();
        if (provider != null && !isBlank(provider.getApiKey())) {
            customHeaders.put("Authorization", provider.getApiKey());
        }
        return customHeaders;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
