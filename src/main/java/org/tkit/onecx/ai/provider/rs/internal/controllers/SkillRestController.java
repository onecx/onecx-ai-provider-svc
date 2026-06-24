package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.SkillDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.SkillMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.SkillInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateSkillRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.SkillSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateSkillRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class SkillRestController implements SkillInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    SkillDAO dao;

    @Inject
    SkillMapper mapper;

    @Override
    public Response createSkill(CreateSkillRequestDTO createSkillRequestDTO) {
        var item = mapper.create(createSkillRequestDTO);
        item = dao.create(item);
        return Response.status(Response.Status.CREATED).entity(mapper.map(item)).build();
    }

    @Override
    public Response deleteSkillById(String id) {
        dao.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findSkillByCriteria(SkillSearchCriteriaDTO skillSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(skillSearchCriteriaDTO);
        var result = dao.findSkillsByCriteriaIncludingGlobal(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getSkillById(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
    }

    @Override
    public Response updateSkillById(String id, UpdateSkillRequestDTO updateSkillRequestDTO) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(item, updateSkillRequestDTO);
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
