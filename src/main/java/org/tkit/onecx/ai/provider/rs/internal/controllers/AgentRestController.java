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
import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentGroupDAO;
import org.tkit.onecx.ai.provider.domain.daos.ModelDAO;
import org.tkit.onecx.ai.provider.domain.daos.RuntimeConfigDAO;
import org.tkit.onecx.ai.provider.domain.daos.ScaffoldDAO;
import org.tkit.onecx.ai.provider.domain.daos.ToolDAO;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.RuntimeConfig;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.rs.internal.mappers.AgentGroupMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.AgentMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ModelMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.RuntimeConfigMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ScaffoldMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ToolMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.AgentInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.AgentSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateAgentRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateAgentRequestDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Transactional(value = NOT_SUPPORTED)
public class AgentRestController implements AgentInternalApi {

    @Inject
    AgentDAO dao;

    @Inject
    ToolDAO toolDAO;

    @Inject
    ModelDAO modelDAO;

    @Inject
    ScaffoldDAO scaffoldDAO;

    @Inject
    RuntimeConfigDAO runtimeConfigDAO;

    @Inject
    AgentGroupDAO agentGroupDAO;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    AgentMapper mapper;

    @Inject
    ToolMapper toolMapper;

    @Inject
    ModelMapper modelMapper;

    @Inject
    ScaffoldMapper scaffoldMapper;

    @Inject
    RuntimeConfigMapper runtimeConfigMapper;

    @Inject
    AgentGroupMapper agentGroupMapper;

    @Inject
    AgentService agentService;

    @Override
    public Response createAgent(CreateAgentRequestDTO createAgentRequestDTO) {
        var context = agentService.createAgent(mapper.mapCreate(createAgentRequestDTO));
        return Response.status(Response.Status.CREATED).entity(mapper.map(context)).build();
    }

    @Override
    public Response deleteAgent(String id) {
        dao.deleteQueryById(id);
        return Response.noContent().build();
    }

    @Override
    public Response findAgentBySearchCriteria(AgentSearchCriteriaDTO agentSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(agentSearchCriteriaDTO);
        var result = dao.findAgentsByCriteria(criteria);
        return Response.ok(mapper.mapPage(result)).build();
    }

    @Override
    public Response getAgent(String id) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(mapper.map(item)).build();
    }

    @Override
    @Transactional
    public Response updateAgent(String id, UpdateAgentRequestDTO updateAgentRequestDTO) {
        var item = dao.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Resolve tools
        var toolsToAdd = new HashSet<Tool>();
        if (updateAgentRequestDTO.getTools() != null) {
            updateAgentRequestDTO.getTools().forEach(tool -> {
                var existing = toolDAO.findById(tool.getId());
                if (existing == null) {
                    existing = toolDAO.create(toolMapper.map(tool));
                }
                toolsToAdd.add(existing);
            });
        }

        // Resolve model
        Model model = null;
        if (updateAgentRequestDTO.getModel() != null) {
            var modelId = updateAgentRequestDTO.getModel().getId();
            model = modelId != null ? modelDAO.findById(modelId) : null;
            if (model == null) {
                model = modelDAO.create(modelMapper.map(updateAgentRequestDTO.getModel()));
            }
        }

        // Resolve scaffold
        Scaffold scaffold = null;
        if (updateAgentRequestDTO.getScaffold() != null) {
            var scaffoldId = updateAgentRequestDTO.getScaffold().getId();
            scaffold = scaffoldId != null ? scaffoldDAO.findById(scaffoldId) : null;
            if (scaffold == null) {
                scaffold = scaffoldDAO.create(scaffoldMapper.map(updateAgentRequestDTO.getScaffold()));
            }
        }

        // Resolve runtimeConfig
        RuntimeConfig runtimeConfig = null;
        if (updateAgentRequestDTO.getRuntimeConfig() != null) {
            var rtId = updateAgentRequestDTO.getRuntimeConfig().getId();
            runtimeConfig = rtId != null ? runtimeConfigDAO.findById(rtId) : null;
            if (runtimeConfig == null) {
                runtimeConfig = runtimeConfigDAO.create(runtimeConfigMapper.map(updateAgentRequestDTO.getRuntimeConfig()));
            }
        }

        // Resolve groups
        var groupsToAdd = new HashSet<AgentGroup>();
        if (updateAgentRequestDTO.getGroups() != null) {
            updateAgentRequestDTO.getGroups().forEach(groupDto -> {
                var existing = agentGroupDAO.findById(groupDto.getId());
                if (existing == null) {
                    existing = agentGroupDAO.create(agentGroupMapper.map(groupDto));
                }
                groupsToAdd.add(existing);
            });
        }

        mapper.mapUpdate(item, updateAgentRequestDTO, toolsToAdd, model, scaffold, runtimeConfig, groupsToAdd);
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
