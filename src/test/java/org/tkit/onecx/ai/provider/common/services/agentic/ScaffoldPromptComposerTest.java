package org.tkit.onecx.ai.provider.common.services.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Skill;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentFilterDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.RequestContextDTOV1;

class ScaffoldPromptComposerTest {

    private final ScaffoldPromptComposer composer = new ScaffoldPromptComposer();

    @Test
    void compose_includesScaffoldSkillsInDeterministicOrder() {
        Scaffold scaffold = new Scaffold();
        scaffold.setSystemPrompt("Base system prompt");
        scaffold.setSkills(Set.of(
                skill("Zoo", "Animal facts", "Use zoology knowledge."),
                skill("Audit", "Compliance checks", "Use compliance rules.")));

        Agent agent = new Agent();
        agent.setScaffold(scaffold);
        agent.setAdditionalPrompt("Agent prompt");

        String prompt = composer.compose(agent, null);

        assertThat(prompt).contains("Base system prompt");
        assertThat(prompt).contains("Available scaffold skills. Use only when relevant:");
        assertThat(prompt).contains("- Audit: Compliance checks");
        assertThat(prompt).contains("Instruction: Use compliance rules.");
        assertThat(prompt).contains("- Zoo: Animal facts");
        assertThat(prompt).contains("Instruction: Use zoology knowledge.");
        assertThat(prompt).contains("Agent prompt");
        assertThat(prompt.indexOf("- Audit")).isLessThan(prompt.indexOf("- Zoo"));
    }

    @Test
    void compose_withoutSkills_keepsExistingPromptShape() {
        Scaffold scaffold = new Scaffold();
        scaffold.setSystemPrompt("Base system prompt");

        Agent agent = new Agent();
        agent.setScaffold(scaffold);
        agent.setAdditionalPrompt("Agent prompt");

        assertThat(composer.compose(agent, null)).isEqualTo("Base system prompt\n\nAgent prompt");
    }

    @Test
    void compose_includesRequestContextDirective() {
        Agent agent = new Agent();
        agent.setAdditionalPrompt("Agent prompt");

        ChatRequestDTOV1 request = new ChatRequestDTOV1();
        RequestContextDTOV1 context = new RequestContextDTOV1();
        AgentFilterDTOV1 filter = new AgentFilterDTOV1();
        filter.setKey(AgentFilterDTOV1.KeyEnum.APP_ID);
        filter.setValue("onecx");
        context.setFilter(filter);
        request.setRequestContext(context);

        assertThat(composer.compose(agent, request))
                .isEqualTo("Agent prompt\n\nRequest context filter: APP_ID=onecx");
    }

    private Skill skill(String name, String description, String instruction) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setDescription(description);
        skill.setInstruction(instruction);
        return skill;
    }
}
