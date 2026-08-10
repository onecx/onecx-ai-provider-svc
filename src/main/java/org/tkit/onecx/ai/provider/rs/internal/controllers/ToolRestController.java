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
import org.tkit.onecx.ai.provider.domain.daos.GlobalToolDAO;
import org.tkit.onecx.ai.provider.domain.daos.McpToolRuleDAO;
import org.tkit.onecx.ai.provider.domain.daos.ToolDAO;
import org.tkit.onecx.ai.provider.domain.models.AbstractTool;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.McpToolRule;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.DangerLevel;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.McpToolRuleMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ToolMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.ToolInternalApi;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateMcpToolRuleRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateToolRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DangerLevelDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DiscoveredToolAnnotationsDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DiscoveredToolInfoDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DiscoveredToolInfoListDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.McpToolRuleListDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ToolSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateMcpToolRuleRequestDTO;
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
    McpToolRuleDAO mcpToolRuleDAO;

    @Inject
    DangerClassificationService dangerClassificationService;

    @Inject
    McpToolRuleMapper ruleMapper;

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
        mcpToolRuleDAO.deleteByToolId(id);
        mcpToolRuleDAO.deleteByGlobalToolId(id);
        toolDAO.deleteQueryById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response getDiscoveredTools(String toolId) {
        AbstractTool tool = toolDAO.findById(toolId);
        boolean global = false;
        if (tool == null) {
            tool = globalToolDAO.findById(toolId);
            global = tool != null;
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

        List<McpToolRule> existingRules = global
                ? mcpToolRuleDAO.findByGlobalToolId(toolId)
                : mcpToolRuleDAO.findByToolId(toolId);
        Map<String, McpToolRule> rulesByName = existingRules.stream()
                .collect(Collectors.toMap(McpToolRule::getToolName, r -> r, (a, b) -> a));
        Set<String> discoveredNames = discovered.stream()
                .map(DiscoveredTool::getName)
                .collect(Collectors.toSet());

        var result = new DiscoveredToolInfoListDTO();
        List<DiscoveredToolInfoDTO> infos = new java.util.ArrayList<>();
        for (var dt : discovered) {
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
            McpToolRule rule = rulesByName.get(dt.getName());
            if (rule != null) {
                info.setExistingRule(ruleMapper.map(rule));
            }
            info.setOrphaned(false);
            infos.add(info);
        }
        for (McpToolRule rule : existingRules) {
            if (!discoveredNames.contains(rule.getToolName())) {
                var info = new DiscoveredToolInfoDTO();
                info.setName(rule.getToolName());
                info.setDescription(rule.getToolDescription());
                info.setExistingRule(ruleMapper.map(rule));
                info.setOrphaned(true);
                infos.add(info);
            }
        }
        result.setTools(infos);
        return Response.ok(result).build();
    }

    @Override
    public Response getMcpToolRules(String toolId) {
        List<McpToolRule> rules = findRules(toolId);
        if (rules == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var result = new McpToolRuleListDTO();
        result.setRules(ruleMapper.map(rules));
        return Response.ok(result).build();
    }

    @Override
    public Response createMcpToolRule(String toolId, CreateMcpToolRuleRequestDTO createMcpToolRuleRequestDTO) {
        Tool tool = toolDAO.findById(toolId);
        GlobalTool globalTool = null;
        if (tool == null) {
            globalTool = globalToolDAO.findById(toolId);
            if (globalTool == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }
        var rule = ruleMapper.create(createMcpToolRuleRequestDTO);
        rule.setTool(tool);
        rule.setGlobalTool(globalTool);
        rule = mcpToolRuleDAO.create(rule);
        return Response.status(Response.Status.CREATED).entity(ruleMapper.map(rule)).build();
    }

    @Override
    public Response updateMcpToolRule(String toolId, String ruleId,
            UpdateMcpToolRuleRequestDTO updateMcpToolRuleRequestDTO) {
        var rule = mcpToolRuleDAO.findById(ruleId);
        if (rule == null || !belongsToTool(rule, toolId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        ruleMapper.update(rule, updateMcpToolRuleRequestDTO);
        rule = mcpToolRuleDAO.update(rule);
        return Response.ok(ruleMapper.map(rule)).build();
    }

    @Override
    public Response deleteMcpToolRule(String toolId, String ruleId) {
        var rule = mcpToolRuleDAO.findById(ruleId);
        if (rule == null || !belongsToTool(rule, toolId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mcpToolRuleDAO.deleteQueryById(ruleId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private List<McpToolRule> findRules(String toolId) {
        if (toolDAO.findById(toolId) != null) {
            return mcpToolRuleDAO.findByToolId(toolId);
        }
        if (globalToolDAO.findById(toolId) != null) {
            return mcpToolRuleDAO.findByGlobalToolId(toolId);
        }
        return null;
    }

    private boolean belongsToTool(McpToolRule rule, String toolId) {
        if (rule.getTool() != null && toolId.equals(rule.getTool().getId())) {
            return true;
        }
        return rule.getGlobalTool() != null && toolId.equals(rule.getGlobalTool().getId());
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
