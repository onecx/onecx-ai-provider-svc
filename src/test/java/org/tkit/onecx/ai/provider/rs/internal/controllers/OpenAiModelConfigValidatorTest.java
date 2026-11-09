package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

class OpenAiModelConfigValidatorTest {

    private final OpenAiModelConfigValidator validator = new OpenAiModelConfigValidator();

    OpenAiModelConfigValidatorTest() {
        validator.objectMapper = new ObjectMapper();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"api\":\"chat-completions\",\"api\":\"responses\",\"reasoningEffort\":\"medium\"}",
            "{\"api\":\"chat-completions\",\"\\u0061pi\":\"responses\"}",
            "{\"reasoningEffort\":\"low\",\"reasoningEffort\":\"medium\"}",
            "{\"reasoningEffort\":\"low\",\"reasoning\\u0045ffort\":\"medium\"}"
    })
    void rejectsDuplicateDecodedKeys(String config) {
        assertThatThrownBy(() -> validator.validate(config)).isInstanceOf(ModelConfigValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"\\u0061pi\":\"responses\",\"reasoning\\u0045ffort\":\"medium\"}",
            "{\"api\":\"responses\",\"reasoningEffort\":\"api: \\\"responses\\\", \\\"api\\\": \\\"other\\\"\"}",
            "{\"reasoningEffort\":\"colon: and backslash \\\\ and quote \\\"\"}"
    })
    void acceptsUniqueEscapedKeysAndStringContents(String config) {
        assertThatCode(() -> validator.validate(config)).doesNotThrowAnyException();
    }
}
