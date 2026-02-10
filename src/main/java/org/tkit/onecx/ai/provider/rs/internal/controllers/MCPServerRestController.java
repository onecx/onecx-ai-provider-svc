package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.MCPServerDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.MCPServerMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.McpServerInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateMCPServerRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.MCPServerSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateMCPServerRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class MCPServerRestController implements McpServerInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    MCPServerDAO mcpServerDAO;

    @Inject
    MCPServerMapper mapper;

    @Override
    public Response createMCPServer(CreateMCPServerRequestDTO createMCPServerRequestDTO) {
        var mcpServer = mapper.create(createMCPServerRequestDTO);
        mcpServer = mcpServerDAO.create(mcpServer);
        return Response.status(Response.Status.CREATED).entity(mapper.map(mcpServer)).build();
    }

    @Override
    public Response deleteMCPServerById(String id) {
        mcpServerDAO.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findMCPServerByCriteria(MCPServerSearchCriteriaDTO criteriaDTO) {
        var criteria = mapper.mapCriteria(criteriaDTO);
        var result = mcpServerDAO.findMCPServersByCriteria(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getMCPServerById(String id) {
        var mcpServer = mcpServerDAO.findById(id);
        if (mcpServer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = mapper.map(mcpServer);
        return Response.status(Response.Status.OK).entity(dto).build();
    }

    @Override
    public Response updateMCPServerById(String id, UpdateMCPServerRequestDTO updateMCPServerRequestDTO) {
        var mcpServer = mcpServerDAO.findById(id);
        if (mcpServer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(mcpServer, updateMCPServerRequestDTO);
        mcpServer = mcpServerDAO.update(mcpServer);
        return Response.status(Response.Status.OK).entity(mapper.map(mcpServer)).build();
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> optimisticLockException(OptimisticLockException ex) {
        return exceptionMapper.optimisticLock(ex);
    }
}
