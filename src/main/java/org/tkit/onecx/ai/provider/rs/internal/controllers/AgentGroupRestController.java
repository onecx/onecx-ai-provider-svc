package org.tkit.onecx.ai.provider.rs.internal.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.domain.daos.AgentGroupDAO;
import org.tkit.onecx.ai.provider.rs.internal.mappers.AgentGroupMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.AgentGroupInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.AgentGroupSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateAgentGroupRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateAgentGroupRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class AgentGroupRestController implements AgentGroupInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    AgentGroupDAO dao;

    @Inject
    AgentGroupMapper mapper;

    @Override
    public Response createAgentGroup(CreateAgentGroupRequestDTO createAgentGroupRequestDTO) {
        var item = mapper.create(createAgentGroupRequestDTO);
        item = dao.create(item);
        return Response.status(Response.Status.CREATED).entity(mapper.map(item)).build();
    }

    @Override
    public Response deleteAgentGroupById(String id) {
        dao.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findAgentGroupByCriteria(AgentGroupSearchCriteriaDTO agentGroupSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(agentGroupSearchCriteriaDTO);
        var result = dao.findAgentGroupsByCriteria(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getAgentGroupById(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(mapper.map(item)).build();
    }

    @Override
    public Response updateAgentGroupById(String id, UpdateAgentGroupRequestDTO updateAgentGroupRequestDTO) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(item, updateAgentGroupRequestDTO);
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
