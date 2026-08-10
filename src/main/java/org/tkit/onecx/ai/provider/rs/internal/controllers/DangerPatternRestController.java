package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.DangerPatternDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.DangerPatternMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.DangerPatternInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateDangerPatternRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DangerPatternListDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateDangerPatternRequestDTO;

@ApplicationScoped
public class DangerPatternRestController implements DangerPatternInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    DangerPatternDAO dangerPatternDAO;

    @Inject
    DangerPatternMapper dangerPatternMapper;

    @Override
    public Response getDangerPatterns() {
        var result = new DangerPatternListDTO();
        result.setPatterns(dangerPatternMapper.map(dangerPatternDAO.findAllPatterns()));
        return Response.ok(result).build();
    }

    @Override
    public Response createDangerPattern(CreateDangerPatternRequestDTO createDangerPatternRequestDTO) {
        var pattern = dangerPatternMapper.create(createDangerPatternRequestDTO);
        pattern = dangerPatternDAO.create(pattern);
        return Response.status(Response.Status.CREATED).entity(dangerPatternMapper.map(pattern)).build();
    }

    @Override
    public Response updateDangerPattern(String id, UpdateDangerPatternRequestDTO updateDangerPatternRequestDTO) {
        var pattern = dangerPatternDAO.findById(id);
        if (pattern == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        dangerPatternMapper.update(pattern, updateDangerPatternRequestDTO);
        pattern = dangerPatternDAO.update(pattern);
        return Response.ok(dangerPatternMapper.map(pattern)).build();
    }

    @Override
    public Response deleteDangerPattern(String id) {
        var pattern = dangerPatternDAO.findById(id);
        if (pattern == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        dangerPatternDAO.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
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
