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
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class AnthropicProviderAdapter implements ProviderAdapter {

    private static final String HEALTH_CHECK_PROMPT = "ping";
    private static final String HEALTH_CHECK_MODEL = "claude-3-haiku-20240307";

    @Inject
    DispatchConfig dispatchConfig;

    @Override
    public boolean supports(ProviderType type) {
        return ProviderType.ANTHROPIC.equals(type);
    }

    @Override
    public ChatModel createChatModel(Agent agent) {
        if (agent == null || agent.getModel() == null || agent.getModel().getProvider() == null) {
            throw new IllegalArgumentException("Agent has no associated model or provider");
        }
        Provider provider = agent.getModel().getProvider();
        String modelName = agent.getModel().getModelIdentifier();
        if (isBlank(provider.getApiKey())) {
            throw new IllegalArgumentException("Anthropic provider has no API key configured");
        }
        if (isBlank(modelName)) {
            throw new IllegalArgumentException("Agent model has no model identifier configured");
        }
        log.info("Creating Anthropic chat model: provider={}, model={}, baseUrl={}, timeoutSeconds={}, maxRetries={}",
                provider.getName(), modelName, provider.getLlmUrl(), providerTimeoutSeconds(), providerMaxRetries());
        return buildModel(provider, modelName);
    }

    @Override
    public String healthCheck(Provider provider) {
        if (provider == null || isBlank(provider.getApiKey())) {
            log.warn("Anthropic provider configuration incomplete for health check");
            return UNHEALTHY;
        }
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(new UserMessage(HEALTH_CHECK_PROMPT)))
                    .build();
            var response = buildModel(provider, HEALTH_CHECK_MODEL).chat(request);
            if (response == null || response.aiMessage() == null) {
                log.warn("Anthropic health check failed for provider '{}'", provider.getName());
                return UNHEALTHY;
            }
            return HEALTHY;
        } catch (Exception e) {
            Throwable rootCause = rootCause(e);
            log.warn(
                    "Anthropic health check failed: provider={}, model={}, baseUrl={}, timeoutSeconds={}, maxRetries={}, errorType={}, message={}",
                    provider.getName(), HEALTH_CHECK_MODEL, provider.getLlmUrl(), providerTimeoutSeconds(),
                    providerMaxRetries(), rootCause.getClass().getSimpleName(), rootCause.getMessage());
            log.debug("Anthropic health check failure details for provider '{}'", provider.getName(), e);
            return UNHEALTHY;
        }
    }

    private AnthropicChatModel buildModel(Provider provider, String modelName) {
        var builder = AnthropicChatModel.builder()
                .apiKey(provider.getApiKey())
                .modelName(modelName)
                .timeout(Duration.ofSeconds(providerTimeoutSeconds()))
                .maxRetries(providerMaxRetries())
                .logRequests(dispatchConfig.providerConfig().logRequests())
                .logResponses(dispatchConfig.providerConfig().logResponse());

        String baseUrl = provider.getLlmUrl();
        if (!isBlank(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private long providerTimeoutSeconds() {
        return dispatchConfig != null && dispatchConfig.providerConfig() != null
                ? dispatchConfig.providerConfig().timeout()
                : 60;
    }

    private int providerMaxRetries() {
        long configured = dispatchConfig != null && dispatchConfig.providerConfig() != null
                ? dispatchConfig.providerConfig().maxRetries()
                : 2;
        if (configured < 0) {
            log.warn("Invalid provider max-retries={}; using 0", configured);
            return 0;
        }
        if (configured > Integer.MAX_VALUE) {
            log.warn("Provider max-retries={} exceeds supported range; using {}", configured, Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
        return (int) configured;
    }

    private Throwable rootCause(Throwable throwable) {
        if (throwable == null) {
            return new RuntimeException("unknown failure");
        }
        Throwable result = throwable;
        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }
        return result;
    }
}
