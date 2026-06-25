package org.tkit.onecx.ai.provider.common.services.agentic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Skill;

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
            addIfNotBlank(blocks, buildSkillDirective(agent.getScaffold().getSkills()));
        }

        if (agent != null) {
            addIfNotBlank(blocks, agent.getAdditionalPrompt());
        }

        String requestContextDirective = buildRequestContextDirective(chatRequestDTO);
        addIfNotBlank(blocks, requestContextDirective);

        return String.join("\n\n", blocks);
    }

    private String buildSkillDirective(Iterable<Skill> skills) {
        if (skills == null) {
            return null;
        }

        List<Skill> orderedSkills = new ArrayList<>();
        skills.forEach(orderedSkills::add);
        orderedSkills = orderedSkills.stream()
                .filter(skill -> skill != null
                        && (!isBlank(skill.getName()) || !isBlank(skill.getDescription()) || !isBlank(skill.getInstruction())))
                .sorted(Comparator.comparing(skill -> normalize(skill.getName()).toLowerCase()))
                .toList();

        if (orderedSkills.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("Available scaffold skills. Use only when relevant:");
        for (Skill skill : orderedSkills) {
            sb.append(System.lineSeparator()).append("- ");
            sb.append(!isBlank(skill.getName()) ? normalize(skill.getName()) : "Unnamed skill");
            if (!isBlank(skill.getDescription())) {
                sb.append(": ").append(normalize(skill.getDescription()));
            }
            if (!isBlank(skill.getInstruction())) {
                sb.append(System.lineSeparator())
                        .append("  Instruction: ")
                        .append(normalize(skill.getInstruction()));
            }
        }
        return sb.toString();
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
