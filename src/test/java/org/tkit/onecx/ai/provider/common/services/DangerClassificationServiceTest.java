package org.tkit.onecx.ai.provider.common.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.daos.DangerPatternDAO;
import org.tkit.onecx.ai.provider.domain.models.DangerPattern;
import org.tkit.onecx.ai.provider.domain.models.enums.DangerLevel;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DangerClassificationServiceTest extends AbstractTest {

    @InjectMock
    DangerPatternDAO dao;

    @jakarta.inject.Inject
    DangerClassificationService service;

    @Test
    void classify_returnsDangerous_whenDestructiveHint() {
        assertThat(service.classify("readSomething", null, null, true)).isEqualTo(DangerLevel.DANGEROUS);
    }

    @Test
    void classify_returnsSafe_whenReadOnlyHint() {
        assertThat(service.classify("deleteAll", null, true, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void classify_annotationsTakePrecedenceOverPatterns() {
        when(dao.findAllPatterns()).thenReturn(List.of(pattern("delete", DangerLevel.DANGEROUS)));
        assertThat(service.classify("deleteItem", null, null, false)).isEqualTo(DangerLevel.DANGEROUS);
        assertThat(service.classify("deleteItem", null, true, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void classify_matchesCamelCaseSegments() {
        when(dao.findAllPatterns()).thenReturn(List.of(pattern("delete", DangerLevel.DANGEROUS)));
        assertThat(service.classify("deleteProposal", null, null, null)).isEqualTo(DangerLevel.DANGEROUS);
        assertThat(service.classify("delete_proposal", null, null, null)).isEqualTo(DangerLevel.DANGEROUS);
        assertThat(service.classify("undeleteProposal", null, null, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void classify_picksHighestMatchingLevel() {
        when(dao.findAllPatterns()).thenReturn(List.of(
                pattern("import", DangerLevel.WARNING),
                pattern("delete", DangerLevel.DANGEROUS)));
        assertThat(service.classify("importDelete", null, null, null)).isEqualTo(DangerLevel.DANGEROUS);
    }

    @Test
    void classify_matchesDescription() {
        when(dao.findAllPatterns()).thenReturn(List.of(pattern("drop", DangerLevel.DANGEROUS)));
        assertThat(service.classify("cleanup", "Drop all data", null, null)).isEqualTo(DangerLevel.DANGEROUS);
        assertThat(service.classify("cleanup", "Drops all data", null, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void classify_defaultsToSafe_whenNoMatch() {
        when(dao.findAllPatterns()).thenReturn(List.of(pattern("delete", DangerLevel.DANGEROUS)));
        assertThat(service.classify("getProposal", "Reads a proposal", null, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void classify_skipsNullOrIncompletePatterns() {
        var incomplete = new DangerPattern();
        incomplete.setPattern(null);
        when(dao.findAllPatterns()).thenReturn(List.of(incomplete, pattern(null, null), pattern("delete", null)));
        assertThat(service.classify("anything", null, null, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void matches_handlesBlankText() {
        assertThat(service.matches("delete", null)).isFalse();
        assertThat(service.matches("delete", "  ")).isFalse();
    }

    @Test
    void classify_returnsWarning_whenOpenWorldHint() {
        assertThat(service.classify("safeTool", null, null, null, null, true)).isEqualTo(DangerLevel.WARNING);
    }

    @Test
    void classify_returnsSafe_whenIdempotentHint() {
        assertThat(service.classify("safeTool", null, null, null, true, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void classify_fiveArgOverload_delegatesToSixArg() {
        assertThat(service.classify("deleteItem", null, null, true, true, null)).isEqualTo(DangerLevel.DANGEROUS);
    }

    @Test
    void classify_defaultsToSafe_whenAllHintsNullAndNoPatternMatch() {
        when(dao.findAllPatterns()).thenReturn(List.of());
        assertThat(service.classify("unknownTool", "no match", null, null, null, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void classify_destructiveTakesPrecedenceOverOpenWorld() {
        assertThat(service.classify("dangerousTool", null, null, true, null, true)).isEqualTo(DangerLevel.DANGEROUS);
    }

    @Test
    void classify_readOnlyTakesPrecedenceOverPatterns() {
        when(dao.findAllPatterns()).thenReturn(List.of(pattern("delete", DangerLevel.DANGEROUS)));
        assertThat(service.classify("deleteItem", null, true, null, null, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void segments_splitsCamelCaseAndNonAlphanumeric() {
        assertThat(service.segments("deleteProposal")).contains("delete", "proposal");
        assertThat(service.segments("delete_proposal_v2")).contains("delete", "proposal", "v2");
        assertThat(service.segments("DELETE-PROPOSAL")).contains("delete", "proposal");
        assertThat(service.segments("")).isEmpty();
    }

    @Test
    void classify_doesNotReplaceHigherLevelWithLowerMatch() {
        // "delete" (DANGEROUS) matches first, then "import" (WARNING) also matches
        // but WARNING.ordinal() < DANGEROUS.ordinal() → result stays DANGEROUS
        when(dao.findAllPatterns()).thenReturn(List.of(
                pattern("delete", DangerLevel.DANGEROUS),
                pattern("import", DangerLevel.WARNING)));
        assertThat(service.classify("deleteImport", null, null, null)).isEqualTo(DangerLevel.DANGEROUS);
    }

    @Test
    void classify_matchesToolNameOnly_withExistingResult_ordinalGreaterThanResult() {
        // First pattern matches toolName (result=null → set), second pattern also matches
        // toolName only (not description) with higher ordinal → replaces result
        when(dao.findAllPatterns()).thenReturn(List.of(
                pattern("import", DangerLevel.WARNING),
                pattern("delete", DangerLevel.DANGEROUS)));
        assertThat(service.classify("deleteImport", "no match here", null, null, null, null))
                .isEqualTo(DangerLevel.DANGEROUS);
    }

    @Test
    void classify_matchesDescriptionOnly_withExistingResult_ordinalNotGreaterThanResult() {
        // First pattern matches toolName (result=DANGEROUS), second matches description only
        // with lower ordinal → does not replace result
        when(dao.findAllPatterns()).thenReturn(List.of(
                pattern("delete", DangerLevel.DANGEROUS),
                pattern("import", DangerLevel.WARNING)));
        assertThat(service.classify("deleteTool", "import data", null, null, null, null))
                .isEqualTo(DangerLevel.DANGEROUS);
    }

    private static DangerPattern pattern(String pattern, DangerLevel level) {
        var p = new DangerPattern();
        p.setPattern(pattern);
        p.setDangerLevel(level);
        return p;
    }
}
