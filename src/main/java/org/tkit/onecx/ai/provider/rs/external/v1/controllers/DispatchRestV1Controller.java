package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.services.llm.LlmServiceFactory;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.DispatchV1Api;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class DispatchRestV1Controller implements DispatchV1Api {

    @Inject
    LlmServiceFactory llmServiceFactory;

    @Override
    public Response chat(ChatRequestDTOV1 chatRequestDTOV1) {
        return llmServiceFactory.chat(chatRequestDTOV1);
    }
}
