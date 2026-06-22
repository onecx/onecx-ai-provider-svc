package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.ModelDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ModelMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ModelInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateModelRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ModelSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateModelRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ModelRestController implements ModelInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ModelDAO dao;

    @Inject
    ModelMapper mapper;

    @Override
    public Response createModel(CreateModelRequestDTO createModelRequestDTO) {
        var item = mapper.create(createModelRequestDTO);
        item = dao.create(item);
        return Response.status(Response.Status.CREATED).entity(mapper.map(item)).build();
    }

    @Override
    public Response deleteModelById(String id) {
        dao.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findModelByCriteria(ModelSearchCriteriaDTO modelSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(modelSearchCriteriaDTO);
        var result = dao.findModelsByCriteria(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getModelById(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
    }

    @Override
    public Response updateModelById(String id, UpdateModelRequestDTO updateModelRequestDTO) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(item, updateModelRequestDTO);
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
