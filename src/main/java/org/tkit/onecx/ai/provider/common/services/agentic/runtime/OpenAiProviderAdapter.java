package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class OpenAiProviderAdapter implements ProviderAdapter {

    private static final String HEALTH_CHECK_PROMPT = "ping";
    private static final String HEALTH_CHECK_MODEL = "gpt-4o-mini";

    @Inject
    DispatchConfig dispatchConfig;

    @Override
    public boolean supports(ProviderType type) {
        return ProviderType.OPENAI.equals(type);
    }

    @Override
    public ChatModel createChatModel(Agent agent) {
        if (agent == null || agent.getModel() == null || agent.getModel().getProvider() == null) {
            throw new IllegalArgumentException("Agent has no associated model or provider");
        }
        Provider provider = agent.getModel().getProvider();
        String modelName = agent.getModel().getModelIdentifier();
        if (isBlank(provider.getApiKey())) {
            throw new IllegalArgumentException("OpenAI provider has no API key configured");
        }
        if (isBlank(modelName)) {
            throw new IllegalArgumentException("Agent model has no model identifier configured");
        }
        return buildModel(provider, modelName);
    }

    @Override
    public String healthCheck(Provider provider) {
        if (provider == null || isBlank(provider.getApiKey())) {
            log.warn("OpenAI provider configuration incomplete for health check");
            return UNHEALTHY;
        }
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(new UserMessage(HEALTH_CHECK_PROMPT)))
                    .build();
            var response = buildModel(provider, HEALTH_CHECK_MODEL).chat(request);
            if (response == null || response.aiMessage() == null) {
                log.warn("OpenAI health check failed for provider '{}'", provider.getName());
                return UNHEALTHY;
            }
            return HEALTHY;
        } catch (Exception e) {
            log.warn("OpenAI health check failed for provider '{}': {}", provider.getName(), e.getMessage());
            log.debug("OpenAI health check failure details for provider '{}'", provider.getName(), e);
            return UNHEALTHY;
        }
    }

    private OpenAiChatModel buildModel(Provider provider, String modelName) {
        var builder = OpenAiChatModel.builder()
                .apiKey(provider.getApiKey())
                .modelName(modelName)
                .timeout(Duration.ofSeconds(dispatchConfig.providerConfig().timeout()))
                .logRequests(dispatchConfig.providerConfig().logRequests())
                .logResponses(dispatchConfig.providerConfig().logResponse());

        if (!isBlank(provider.getLlmUrl())) {
            builder.baseUrl(provider.getLlmUrl());
        }
        return builder.build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
