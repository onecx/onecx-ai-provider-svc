package org.tkit.onecx.ai.provider.common.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.domain.daos.DangerPatternDAO;
import org.tkit.onecx.ai.provider.domain.models.DangerPattern;
import org.tkit.onecx.ai.provider.domain.models.enums.DangerLevel;

@ApplicationScoped
public class DangerClassificationService {

    @Inject
    DangerPatternDAO dangerPatternDAO;

    public DangerLevel classify(String toolName, String toolDescription, Boolean readOnlyHint,
            Boolean destructiveHint) {
        return classify(toolName, toolDescription, readOnlyHint, destructiveHint, null, null);
    }

    public DangerLevel classify(String toolName, String toolDescription, Boolean readOnlyHint,
            Boolean destructiveHint, Boolean idempotentHint, Boolean openWorldHint) {
        if (Boolean.TRUE.equals(destructiveHint)) {
            return DangerLevel.DANGEROUS;
        }
        if (Boolean.TRUE.equals(openWorldHint)) {
            return DangerLevel.WARNING;
        }
        if (Boolean.TRUE.equals(readOnlyHint) || Boolean.TRUE.equals(idempotentHint)) {
            return DangerLevel.SAFE;
        }
        DangerLevel byPatterns = classifyByPatterns(toolName, toolDescription);
        if (byPatterns != null) {
            return byPatterns;
        }
        return DangerLevel.SAFE;
    }

    DangerLevel classifyByPatterns(String toolName, String toolDescription) {
        List<DangerPattern> patterns = dangerPatternDAO.findAllPatterns();
        DangerLevel result = null;
        for (DangerPattern pattern : patterns) {
            if (pattern.getPattern() == null || pattern.getDangerLevel() == null) {
                continue;
            }
            if (matches(pattern.getPattern(), toolName) || matches(pattern.getPattern(), toolDescription)) {
                if (result == null || pattern.getDangerLevel().ordinal() > result.ordinal()) {
                    result = pattern.getDangerLevel();
                }
            }
        }
        return result;
    }

    boolean matches(String pattern, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalizedPattern = pattern.toLowerCase(Locale.ROOT);
        for (String segment : segments(text)) {
            if (segment.equals(normalizedPattern)) {
                return true;
            }
        }
        return false;
    }

    List<String> segments(String text) {
        String[] parts = text.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT)
                .split("[^a-z0-9]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }
}
