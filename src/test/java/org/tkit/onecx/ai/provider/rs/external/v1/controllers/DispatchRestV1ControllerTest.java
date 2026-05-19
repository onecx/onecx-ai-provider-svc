package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.llm.LlmServiceFactory;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class DispatchRestV1ControllerTest {

    @Inject
    DispatchRestV1Controller controller;

    @InjectMock
    LlmServiceFactory llmServiceFactory;

    @Test
    void chat_delegatesToFactory() {
        ChatRequestDTOV1 request = new ChatRequestDTOV1();
        ChatMessageDTOV1 message = new ChatMessageDTOV1();
        message.setType(ChatMessageDTOV1.TypeEnum.USER);
        message.setMessage("hello");
        request.setChatMessage(message);
        Response factoryResponse = Response.ok("ok").build();

        when(llmServiceFactory.chat(request)).thenReturn(factoryResponse);

        try (Response response = controller.chat(request)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.getEntity()).isEqualTo("ok");
        }

        verify(llmServiceFactory).chat(request);
    }
}
