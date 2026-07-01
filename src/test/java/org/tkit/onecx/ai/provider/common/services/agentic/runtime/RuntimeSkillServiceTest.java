package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Skill;

class RuntimeSkillServiceTest {

    private final RuntimeSkillService service = new RuntimeSkillService();

    @Test
    void runtimeSkills_mapsValidScaffoldSkillsWithoutInliningContent() {
        Agent agent = agentWithSkills(
                skill("Zoo", "Animal facts", "Use zoology knowledge."),
                skill("Audit", "Compliance checks", "Use compliance rules."));

        var runtimeSkills = service.runtimeSkills(agent);

        assertThat(runtimeSkills).isNotNull();
        assertThat(runtimeSkills.formatAvailableSkills()).contains("Audit", "Compliance checks");
        assertThat(runtimeSkills.formatAvailableSkills()).contains("Zoo", "Animal facts");
        assertThat(runtimeSkills.formatAvailableSkills()).doesNotContain("Use compliance rules.");
        assertThat(runtimeSkills.formatAvailableSkills()).doesNotContain("Use zoology knowledge.");
        assertThat(runtimeSkills.toolProvider()).isNotNull();
    }

    @Test
    void runtimeSkills_skipsInvalidSkillsAndUsesNameAsFallbackDescription() {
        Agent agent = agentWithSkills(
                skill("Valid", "", "Use valid skill instructions."),
                skill("", "Missing name", "Should be skipped."),
                skill("Missing instruction", "No instruction", ""));

        var runtimeSkills = service.runtimeSkills(agent);

        assertThat(runtimeSkills).isNotNull();
        assertThat(runtimeSkills.formatAvailableSkills()).contains("Valid");
        assertThat(runtimeSkills.formatAvailableSkills()).doesNotContain("Missing name");
        assertThat(runtimeSkills.formatAvailableSkills()).doesNotContain("Missing instruction");
    }

    @Test
    void runtimeSkills_withoutValidSkillsReturnsNull() {
        Agent agent = agentWithSkills(
                skill("", "Missing name", "Should be skipped."),
                skill("Missing instruction", "No instruction", ""));

        assertThat(service.runtimeSkills(agent)).isNull();
    }

    @Test
    void activationPrompt_containsCatalogAndActivationInstruction() {
        var runtimeSkills = service
                .runtimeSkills(agentWithSkills(skill("Audit", "Compliance checks", "Use compliance rules.")));

        assertThat(service.activationPrompt(runtimeSkills))
                .contains("Available skills:")
                .contains("Audit")
                .contains("Activate a relevant skill before applying its instructions.")
                .doesNotContain("Use compliance rules.");
    }

    private Agent agentWithSkills(Skill... skills) {
        Scaffold scaffold = new Scaffold();
        scaffold.setSkills(Set.of(skills));

        Agent agent = new Agent();
        agent.setName("test-agent");
        agent.setScaffold(scaffold);
        return agent;
    }

    private Skill skill(String name, String description, String instruction) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setDescription(description);
        skill.setInstruction(instruction);
        return skill;
    }
}
