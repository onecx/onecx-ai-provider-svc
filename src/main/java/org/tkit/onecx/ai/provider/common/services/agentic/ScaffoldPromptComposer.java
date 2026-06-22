package org.tkit.onecx.ai.provider.common.services.agentic;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.onecx.ai.provider.domain.models.Agent;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;

@ApplicationScoped
public class ScaffoldPromptComposer {

    private static final String REQUEST_CONTEXT_PREFIX = "Request context filter";

    /**
     * Builds a deterministic system prompt from scaffold + agent prompt + request-context directives.
     */
    public String compose(Agent agent, ChatRequestDTOV1 chatRequestDTO) {
        List<String> blocks = new ArrayList<>();

        if (agent != null && agent.getScaffold() != null) {
            addIfNotBlank(blocks, agent.getScaffold().getSystemPrompt());
        }

        if (agent != null) {
            addIfNotBlank(blocks, agent.getAdditionalPrompt());
        }

        String requestContextDirective = buildRequestContextDirective(chatRequestDTO);
        addIfNotBlank(blocks, requestContextDirective);

        return String.join("\n\n", blocks);
    }

    private String buildRequestContextDirective(ChatRequestDTOV1 chatRequestDTO) {
        if (chatRequestDTO == null || chatRequestDTO.getRequestContext() == null
                || chatRequestDTO.getRequestContext().getFilter() == null) {
            return null;
        }

        String key = chatRequestDTO.getRequestContext().getFilter().getKey() != null
                ? chatRequestDTO.getRequestContext().getFilter().getKey().toString()
                : null;
        String value = chatRequestDTO.getRequestContext().getFilter().getValue();

        if (isBlank(key) || isBlank(value)) {
            return null;
        }

        return REQUEST_CONTEXT_PREFIX + ": " + normalize(key) + "=" + normalize(value);
    }

    private void addIfNotBlank(List<String> blocks, String value) {
        if (!isBlank(value)) {
            blocks.add(normalize(value));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
