package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.common.services.configuration.ConfigurationService;
import org.tkit.onecx.ai.provider.domain.daos.ConfigurationDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ConfigurationMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ConfigurationInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ConfigurationSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateConfigurationRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateConfigurationRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Transactional(value = NOT_SUPPORTED)
public class ConfigurationRestController implements ConfigurationInternalApi {

    @Inject
    ConfigurationDAO dao;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ConfigurationMapper mapper;

    @Inject
    ConfigurationService configurationService;

    @Override
    public Response createConfiguration(CreateConfigurationRequestDTO createConfigurationRequestDTO) {
        var context = configurationService.createConfiguration(mapper.mapCreate(createConfigurationRequestDTO));
        return Response.status(Response.Status.CREATED).entity(mapper.map(context)).build();
    }

    @Override
    public Response deleteConfiguration(String id) {
        dao.deleteQueryById(id);
        return Response.noContent().build();
    }

    @Override
    public Response findConfigurationBySearchCriteria(ConfigurationSearchCriteriaDTO aiConfigurationSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(aiConfigurationSearchCriteriaDTO);
        var result = dao.findAIConfigurationsByCriteria(criteria);
        return Response.ok(mapper.mapPage(result)).build();
    }

    @Override
    public Response getConfiguration(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(mapper.map(item)).build();
    }

    @Override
    public Response updateConfiguration(String id, UpdateConfigurationRequestDTO updateAIConfigurationRequestDTO) {
        var context = configurationService.updateConfiguration(updateAIConfigurationRequestDTO, id);
        if (context == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(mapper.map(context)).build();
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
