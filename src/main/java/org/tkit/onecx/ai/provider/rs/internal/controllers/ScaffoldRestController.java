package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.ScaffoldDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ScaffoldMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ScaffoldInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateScaffoldRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ScaffoldSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateScaffoldRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ScaffoldRestController implements ScaffoldInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ScaffoldDAO dao;

    @Inject
    ScaffoldMapper mapper;

    @Override
    public Response createScaffold(CreateScaffoldRequestDTO createScaffoldRequestDTO) {
        var item = mapper.create(createScaffoldRequestDTO);
        item = dao.create(item);
        return Response.status(Response.Status.CREATED).entity(mapper.map(item)).build();
    }

    @Override
    public Response deleteScaffoldById(String id) {
        dao.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findScaffoldByCriteria(ScaffoldSearchCriteriaDTO scaffoldSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(scaffoldSearchCriteriaDTO);
        var result = dao.findScaffoldsByCriteria(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getScaffoldById(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
    }

    @Override
    public Response updateScaffoldById(String id, UpdateScaffoldRequestDTO updateScaffoldRequestDTO) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(item, updateScaffoldRequestDTO);
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
