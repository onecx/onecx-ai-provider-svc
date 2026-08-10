package org.tkit.onecx.ai.provider.common.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.daos.DangerPatternDAO;
import org.tkit.onecx.ai.provider.domain.models.DangerPattern;
import org.tkit.onecx.ai.provider.domain.models.enums.DangerLevel;

class DangerClassificationServiceTest {

    DangerPatternDAO dao;
    DangerClassificationService service;

    @BeforeEach
    void setUp() {
        dao = mock(DangerPatternDAO.class);
        service = new DangerClassificationService();
        service.dangerPatternDAO = dao;
    }

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
        when(dao.findAllPatterns()).thenReturn(List.of(incomplete, pattern(null, null)));
        assertThat(service.classify("anything", null, null, null)).isEqualTo(DangerLevel.SAFE);
    }

    @Test
    void matches_handlesBlankText() {
        assertThat(service.matches("delete", null)).isFalse();
        assertThat(service.matches("delete", "  ")).isFalse();
    }

    private static DangerPattern pattern(String pattern, DangerLevel level) {
        var p = new DangerPattern();
        p.setPattern(pattern);
        p.setDangerLevel(level);
        return p;
    }
}
