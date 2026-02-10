package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ProviderMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ProviderInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateProviderRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProviderSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateProviderRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Transactional(value = NOT_SUPPORTED)
public class ProviderRestController implements ProviderInternalApi {

    @Inject
    ProviderDAO dao;

    @Inject
    ExceptionMapper exceptionMapper;

    @Context
    UriInfo uriInfo;

    @Inject
    ProviderMapper mapper;

    @Override
    public Response createProvider(CreateProviderRequestDTO aiProviderDTO) {

        var provider = mapper.createProvider(aiProviderDTO);
        provider = dao.create(provider);

        return Response
                .created(uriInfo.getAbsolutePathBuilder().path(provider.getId()).build())
                .entity(mapper.map(provider))
                .build();

    }

    @Override
    public Response deleteProvider(String aiProviderId) {
        dao.deleteQueryById(aiProviderId);
        return Response.noContent().build();
    }

    @Override
    public Response findProviderBySearchCriteria(ProviderSearchCriteriaDTO aiProviderSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(aiProviderSearchCriteriaDTO);
        var result = dao.findProvidersByCriteria(criteria);
        return Response.ok(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response updateProvider(String aiProviderId, UpdateProviderRequestDTO aiProviderDTO) {
        var provider = dao.findById(aiProviderId);
        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        mapper.update(aiProviderDTO, provider);
        provider = dao.update(provider);
        return Response.status(Response.Status.OK).entity(mapper.map(provider)).build();
    }

    @Override
    public Response getProvider(String id) {
        var provider = dao.findById(id);
        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(mapper.map(provider)).build();
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
