package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.ToolDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ToolMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ToolInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateToolRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ToolSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateToolRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ToolRestController implements ToolInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ToolDAO toolDAO;

    @Inject
    ToolMapper mapper;

    @Override
    public Response createTool(CreateToolRequestDTO createToolRequestDTO) {
        var tool = mapper.create(createToolRequestDTO);
        tool = toolDAO.create(tool);
        return Response.status(Response.Status.CREATED).entity(mapper.map(tool)).build();
    }

    @Override
    public Response deleteToolById(String id) {
        toolDAO.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findToolByCriteria(ToolSearchCriteriaDTO criteriaDTO) {
        var criteria = mapper.mapCriteria(criteriaDTO);
        var result = toolDAO.findToolsByCriteria(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getToolById(String id) {
        var tool = toolDAO.findById(id);
        if (tool == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = mapper.map(tool);
        return Response.status(Response.Status.OK).entity(dto).build();
    }

    @Override
    public Response updateToolById(String id, UpdateToolRequestDTO updateToolRequestDTO) {
        var tool = toolDAO.findById(id);
        if (tool == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(tool, updateToolRequestDTO);
        tool = toolDAO.update(tool);
        return Response.status(Response.Status.OK).entity(mapper.map(tool)).build();
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
