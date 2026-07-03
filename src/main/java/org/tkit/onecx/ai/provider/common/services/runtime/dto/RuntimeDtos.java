package org.tkit.onecx.ai.provider.common.services.runtime.dto;

import java.util.List;
import java.util.Map;

public final class RuntimeDtos {

    private RuntimeDtos() {
    }

    public record RuntimeChatRequest(ChatRequest chatRequest, AgentSnapshot rootAgent) {
    }

    public record RuntimeChatResponse(String message, RuntimeStatus status, String errorType, String errorMessage) {
    }

    public enum RuntimeStatus {
        SUCCESS,
        FAILED,
        TIMEOUT
    }

    public record ChatRequest(RequestContext requestContext, ChatMessage chatMessage, Conversation conversation) {
    }

    public record RequestContext(AgentFilter filter, Map<String, String> aiContext) {
    }

    public record AgentFilter(String key, String value) {
    }

    public record ChatMessage(String conversationId, String message, String type, Long creationDate) {
    }

    public record Conversation(String conversationId, List<ChatMessage> history, String conversationType) {
    }

    public record AgentSnapshot(String id, String name, String description, String additionalPrompt, Boolean a2aEnabled,
            String status, ModelSnapshot model, ScaffoldSnapshot scaffold, List<ToolSnapshot> tools,
            List<AgentGroupSnapshot> groups) {
    }

    public record ProviderSnapshot(String id, String name, String type, String description, String llmUrl, String apiKey,
            String authMode) {
    }

    public record ModelSnapshot(String id, String name, String modelIdentifier, String modelConfig,
            String communicationMode, ProviderSnapshot provider) {
    }

    public record ScaffoldSnapshot(String id, String source, String name, String systemPrompt, String sourceProduct,
            List<SkillSnapshot> skills) {
    }

    public record SkillSnapshot(String id, String source, String name, String description, String instruction) {
    }

    public record ToolSnapshot(String id, String source, String name, String description, String type, String url,
            String apiKey, String authMode) {
    }

    public record AgentGroupSnapshot(String id, String name, String description, String routingInstructions,
            String orchestrationMode, String responseStrategy, List<AgentSnapshot> agents,
            List<ExternalAgentSnapshot> externalAgents) {
    }

    public record ExternalAgentSnapshot(String id, String name, String description, String discoveryUrl, String apiKey,
            String authMode, Boolean enabled) {
    }
}
