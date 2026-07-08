package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;

import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.AgentGroupDAO;
import org.tkit.onecx.ai.provider.domain.daos.ExternalAgentDAO;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExternalAgentMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ExternalAgentInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateExternalAgentRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ExternalAgentSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateExternalAgentRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Transactional(value = NOT_SUPPORTED)
public class ExternalAgentRestController implements ExternalAgentInternalApi {
    @Inject
    ExternalAgentDAO dao;
    @Inject
    AgentGroupDAO agentGroupDAO;
    @Inject
    ExternalAgentMapper mapper;
    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    @Transactional
    public Response createExternalAgent(CreateExternalAgentRequestDTO createExternalAgentRequestDTO) {
        var item = mapper.create(createExternalAgentRequestDTO);
        // Resolve groups
        var groups = new HashSet<AgentGroup>();
        if (createExternalAgentRequestDTO.getGroupIds() != null) {
            createExternalAgentRequestDTO.getGroupIds().forEach(groupId -> {
                var group = agentGroupDAO.findById(groupId);
                if (group != null) {
                    groups.add(group);
                }
            });
        }
        item.setGroups(groups);
        item = dao.create(item);
        return Response.status(Response.Status.CREATED).entity(mapper.map(item)).build();
    }

    @Override
    public Response deleteExternalAgentById(String id) {
        dao.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findExternalAgentByCriteria(ExternalAgentSearchCriteriaDTO externalAgentSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(externalAgentSearchCriteriaDTO);
        var result = dao.findExternalAgentsByCriteria(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getExternalAgentById(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
    }

    @Override
    @Transactional
    public Response updateExternalAgentById(String id, UpdateExternalAgentRequestDTO updateExternalAgentRequestDTO) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // Resolve groups
        var groups = new HashSet<AgentGroup>();
        if (updateExternalAgentRequestDTO.getGroupIds() != null) {
            updateExternalAgentRequestDTO.getGroupIds().forEach(groupId -> {
                var group = agentGroupDAO.findById(groupId);
                if (group != null) {
                    groups.add(group);
                }
            });
        }
        mapper.update(item, updateExternalAgentRequestDTO, groups);
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
