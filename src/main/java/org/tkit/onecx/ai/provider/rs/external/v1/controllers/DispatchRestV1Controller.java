package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ProblemDetailResponseDTOV1;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.common.exceptions.ChatExceptionBadRequest;
import org.tkit.onecx.ai.provider.common.exceptions.ChatExceptionNotFound;
import org.tkit.onecx.ai.provider.common.services.llm.LlmServiceFactory;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.DispatchMapper;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.ExceptionMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.DispatchV1Api;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class DispatchRestV1Controller implements DispatchV1Api {

    @Inject
    LlmServiceFactory llmServiceFactory;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    DispatchMapper mapper;

    @Override
    public Response chat(ChatRequestDTOV1 chatRequestDTOV1) {
        var response = llmServiceFactory.chat(mapper.map(chatRequestDTOV1));
        return Response.ok(mapper.create(response)).build();
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTOV1> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response chatNotFoundException(ChatExceptionNotFound ex) {
        return Response.status(Response.Status.NOT_FOUND).entity(ex.getMessage()).build();
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTOV1> chatBadRequestException(ChatExceptionBadRequest ex) {
        return exceptionMapper.chatConstraint(ex);
    }

}
