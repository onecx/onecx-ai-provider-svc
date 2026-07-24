package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentMapper;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.ExceptionMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.AgentV1Api;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentSearchCriteriaDTOV1;

@ApplicationScoped
@Transactional(value = NOT_SUPPORTED)
public class AgentRestV1Controller implements AgentV1Api {

    @Inject
    AgentDAO dao;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    AgentMapper mapper;

    @Override
    public Response findAgentBySearchCriteria(AgentSearchCriteriaDTOV1 agentSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(agentSearchCriteriaDTO);
        var result = dao.findAgentsByCriteria(criteria);
        return Response.ok(mapper.mapPage(result)).build();
    }
}
