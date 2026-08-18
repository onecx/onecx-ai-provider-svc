package org.tkit.onecx.ai.provider.rs.internal.controllers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.provider.common.services.DangerClassificationService;
import org.tkit.onecx.ai.provider.domain.daos.AgentMcpToolRuleDAO;
import org.tkit.onecx.ai.provider.domain.daos.GlobalToolDAO;
import org.tkit.onecx.ai.provider.domain.daos.ToolDAO;
import org.tkit.onecx.ai.provider.domain.models.AbstractTool;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.enums.DangerLevel;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ToolMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ToolInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.AgentMcpToolRuleDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateToolRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DangerLevelDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DiscoveredToolAnnotationsDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DiscoveredToolInfoDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DiscoveredToolInfoListDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ToolSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateToolRequestDTO;
import gen.org.tkit.onecx.ai.provider.runtime.client.api.RuntimeInternalApi;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.DiscoveredTool;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolDiscoveryRequest;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolDiscoveryResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ToolRestController implements ToolInternalApi {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ToolDAO toolDAO;

    @Inject
    GlobalToolDAO globalToolDAO;

    @Inject
    AgentMcpToolRuleDAO agentMcpToolRuleDAO;

    @Inject
    DangerClassificationService dangerClassificationService;

    @Inject
    @RestClient
    RuntimeInternalApi providerRuntimeClient;

    @Inject
    ToolMapper mapper;

    @Override
    public Response createTool(CreateToolRequestDTO createToolRequestDTO) {
        var tool = mapper.create(createToolRequestDTO);
        tool = toolDAO.create(tool);
        return Response.status(Response.Status.CREATED).entity(mapper.map(tool)).build();
    }

    @Override
    public Response deleteToolById(String id) {
        agentMcpToolRuleDAO.deleteByToolId(id);
        agentMcpToolRuleDAO.deleteByGlobalToolId(id);
        toolDAO.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response getDiscoveredTools(String toolId, String agentId) {
        AbstractTool tool = toolDAO.findById(toolId);
        if (tool == null) {
            tool = globalToolDAO.findById(toolId);
        }
        if (tool == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var request = new ToolDiscoveryRequest();
        request.setUrl(tool.getUrl());
        request.setApiKey(tool.getApiKey());
        request.setAuthMode(tool.getAuthMode() != null ? tool.getAuthMode().name() : null);

        List<DiscoveredTool> discovered;
        try (Response response = providerRuntimeClient.discoverTools(request)) {
            var body = response.readEntity(ToolDiscoveryResponse.class);
            discovered = body != null && body.getTools() != null ? body.getTools() : List.of();
        }

        var result = new DiscoveredToolInfoListDTO();
        List<DiscoveredToolInfoDTO> infos = new java.util.ArrayList<>();

        List<AgentMcpToolRule> existingRules = agentId != null && !agentId.isBlank()
                ? agentMcpToolRuleDAO.findByAgentAndToolId(agentId, toolId)
                : List.of();
        Map<String, AgentMcpToolRule> rulesByName = existingRules.stream()
                .collect(Collectors.toMap(AgentMcpToolRule::getToolName, r -> r, (a, b) -> a));
        Set<String> discoveredNames = discovered.stream()
                .map(DiscoveredTool::getName)
                .collect(Collectors.toSet());

        for (var dt : discovered) {
            var info = buildDiscoveredToolInfo(dt);
            AgentMcpToolRule rule = rulesByName.get(dt.getName());
            if (rule != null) {
                info.setExistingRule(agentRuleToDTO(rule));
            }
            info.setOrphaned(false);
            infos.add(info);
        }
        for (AgentMcpToolRule rule : existingRules) {
            if (!discoveredNames.contains(rule.getToolName())) {
                var info = new DiscoveredToolInfoDTO();
                info.setName(rule.getToolName());
                info.setDescription(rule.getToolDescription());
                info.setExistingRule(agentRuleToDTO(rule));
                info.setOrphaned(true);
                infos.add(info);
            }
        }
        result.setTools(infos);
        return Response.ok(result).build();
    }

    private DiscoveredToolInfoDTO buildDiscoveredToolInfo(DiscoveredTool dt) {
        var info = new DiscoveredToolInfoDTO();
        info.setName(dt.getName());
        info.setDescription(dt.getDescription());
        if (dt.getAnnotations() != null) {
            var annotations = new DiscoveredToolAnnotationsDTO();
            annotations.setReadOnlyHint(dt.getAnnotations().getReadOnlyHint());
            annotations.setDestructiveHint(dt.getAnnotations().getDestructiveHint());
            annotations.setIdempotentHint(dt.getAnnotations().getIdempotentHint());
            annotations.setOpenWorldHint(dt.getAnnotations().getOpenWorldHint());
            info.setAnnotations(annotations);
        }
        DangerLevel auto = dangerClassificationService.classify(dt.getName(), dt.getDescription(),
                dt.getAnnotations() != null ? dt.getAnnotations().getReadOnlyHint() : null,
                dt.getAnnotations() != null ? dt.getAnnotations().getDestructiveHint() : null,
                dt.getAnnotations() != null ? dt.getAnnotations().getIdempotentHint() : null,
                dt.getAnnotations() != null ? dt.getAnnotations().getOpenWorldHint() : null);
        info.setAutoDangerLevel(DangerLevelDTO.fromValue(auto.name()));
        return info;
    }

    private AgentMcpToolRuleDTO agentRuleToDTO(AgentMcpToolRule rule) {
        var dto = new AgentMcpToolRuleDTO();
        dto.setId(rule.getId());
        dto.setModificationCount(rule.getModificationCount());
        dto.setToolName(rule.getToolName());
        dto.setToolDescription(rule.getToolDescription());
        dto.setAllowed(gen.org.tkit.onecx.ai.provider.rs.internal.model.ToolPermissionDTO.fromValue(
                rule.getAllowed().name()));
        return dto;
    }

    @Override
    public Response findToolByCriteria(ToolSearchCriteriaDTO criteriaDTO) {
        var criteria = mapper.mapCriteria(criteriaDTO);
        var result = toolDAO.findToolsByCriteriaIncludingGlobal(criteria);
        return Response.status(Response.Status.OK).entity(mapper.mapPageResult(result)).build();
    }

    @Override
    public Response getToolById(String id) {
        var tool = toolDAO.findById(id);
        if (tool == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = mapper.map(tool);
        return Response.status(Response.Status.OK).entity(dto).build();
    }

    @Override
    public Response updateToolById(String id, UpdateToolRequestDTO updateToolRequestDTO) {
        var tool = toolDAO.findById(id);
        if (tool == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mapper.update(tool, updateToolRequestDTO);
        tool = toolDAO.update(tool);
        return Response.status(Response.Status.OK).entity(mapper.map(tool)).build();
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
