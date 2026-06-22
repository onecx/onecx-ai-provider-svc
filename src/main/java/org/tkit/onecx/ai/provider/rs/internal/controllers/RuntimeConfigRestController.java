package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.RuntimeConfigDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.RuntimeConfigMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.RuntimeConfigInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateRuntimeConfigRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.RuntimeConfigSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateRuntimeConfigRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class RuntimeConfigRestController implements RuntimeConfigInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    RuntimeConfigDAO dao;

    @Inject
    RuntimeConfigMapper mapper;

    @Override
    public Response createRuntimeConfig(CreateRuntimeConfigRequestDTO createRuntimeConfigRequestDTO) {
        var item = mapper.create(createRuntimeConfigRequestDTO);
        item = dao.create(item);
        return Response.status(Response.Status.CREATED).entity(mapper.map(item)).build();
    }

    @Override
    public Response deleteRuntimeConfigById(String id) {
        dao.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findRuntimeConfigByCriteria(RuntimeConfigSearchCriteriaDTO runtimeConfigSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(runtimeConfigSearchCriteriaDTO);
        var result = dao.findRuntimeConfigsByCriteria(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getRuntimeConfigById(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
    }

    @Override
    public Response updateRuntimeConfigById(String id, UpdateRuntimeConfigRequestDTO updateRuntimeConfigRequestDTO) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(item, updateRuntimeConfigRequestDTO);
        item = dao.update(item);
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
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
