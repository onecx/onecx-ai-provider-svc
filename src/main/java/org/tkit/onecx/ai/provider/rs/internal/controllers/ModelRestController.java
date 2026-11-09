package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.ModelDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
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
    ProviderDAO providerDAO;

    @Inject
    ModelMapper mapper;

    @Inject
    OpenAiModelConfigValidator modelConfigValidator;

    @Override
    public Response createModel(CreateModelRequestDTO createModelRequestDTO) {
        var item = mapper.create(createModelRequestDTO);
        item.setProvider(resolveProvider(item.getProvider()));
        if (item.getModelConfig() != null && isOpenAi(item.getProvider())) {
            modelConfigValidator.validate(item.getModelConfig());
        }
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
        var existingModelConfig = item.getModelConfig();
        var existingProvider = item.getProvider();
        var modelConfigChanged = updateModelRequestDTO.getModelConfig() != null;
        mapper.update(item, updateModelRequestDTO);
        if (updateModelRequestDTO.getProvider() != null) {
            item.setProvider(resolveProvider(item.getProvider()));
        }
        if (modelConfigChanged) {
            var configProvider = updateModelRequestDTO.getProvider() == null ? existingProvider : item.getProvider();
            if (isOpenAi(configProvider)) {
                modelConfigValidator.validate(item.getModelConfig());
            }
        } else {
            item.setModelConfig(existingModelConfig);
        }
        item = dao.update(item);
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
    }

    private Provider resolveProvider(Provider provider) {
        if (provider == null || provider.getId() == null) {
            return provider;
        }
        var persistedProvider = providerDAO.findById(provider.getId());
        return persistedProvider != null ? persistedProvider : provider;
    }

    private boolean isOpenAi(Provider provider) {
        if (provider == null || provider.getId() == null) {
            return false;
        }
        var persistedProvider = providerDAO.findById(provider.getId());
        return persistedProvider != null && persistedProvider.getType() == ProviderType.OPENAI;
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> invalidModelConfig(ModelConfigValidationException ex) {
        return RestResponse.status(Response.Status.BAD_REQUEST,
                exceptionMapper.exception("INVALID_MODEL_CONFIG", ex.getMessage()));
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
