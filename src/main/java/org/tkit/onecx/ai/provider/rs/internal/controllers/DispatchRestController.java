package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.services.dispatch.ChatDispatchService;
import org.tkit.onecx.ai.provider.rs.internal.mappers.DispatchMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.DispatchInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ChatRequestDTO;

@ApplicationScoped
@Transactional(value = NOT_SUPPORTED)
public class DispatchRestController implements DispatchInternalApi {

    @Inject
    ChatDispatchService chatDispatchService;

    @Inject
    DispatchMapper dispatchMapper;

    @Override
    public Response chat(ChatRequestDTO chatRequestDTO) {
        return chatDispatchService.chat(dispatchMapper.map(chatRequestDTO));
    }
}
