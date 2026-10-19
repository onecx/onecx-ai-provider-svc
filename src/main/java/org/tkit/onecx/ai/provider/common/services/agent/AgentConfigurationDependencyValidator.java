package org.tkit.onecx.ai.provider.common.services.agent;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.domain.daos.GlobalToolDAO;
import org.tkit.onecx.ai.provider.domain.daos.ModelDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.daos.ToolDAO;
import org.tkit.onecx.ai.provider.domain.models.AbstractTool;
import org.tkit.onecx.ai.provider.domain.models.enums.AuthMode;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationDTOV1;

@ApplicationScoped
public class AgentConfigurationDependencyValidator {

    @Inject
    ProviderDAO providerDAO;

    @Inject
    ModelDAO modelDAO;

    @Inject
    ToolDAO toolDAO;

    @Inject
    GlobalToolDAO globalToolDAO;

    public List<String> validate(AgentConfigurationDTOV1 configuration) {
        var errors = new ArrayList<String>();
        if (configuration == null) {
            return List.of("Saved configuration is missing");
        }
        var model = configuration.getModel();
        if (model != null && (blank(model.getId()) || modelDAO.findById(model.getId()) == null)) {
            errors.add("Referenced model '" + model.getId() + "' does not exist");
        }
        var provider = configuration.getProvider();
        if (provider != null) {
            var current = blank(provider.getId()) ? null : providerDAO.findById(provider.getId());
            if (current == null) {
                errors.add("Referenced provider '" + provider.getId() + "' does not exist");
            } else {
                if (current.getType() == null) {
                    errors.add("Referenced provider '" + provider.getId() + "' has no type");
                }
                validateCredential(errors, "provider", provider.getId(), provider.getCredentialRef(),
                        current.getAuthMode() == AuthMode.API_KEY || "API_KEY".equals(provider.getAuthMode()),
                        current.getApiKey());
            }
        } else if (model != null) {
            errors.add("Referenced model has no provider configuration");
        }
        if (configuration.getMcpServers() != null) {
            for (var server : configuration.getMcpServers()) {
                if (server == null || blank(server.getId())) {
                    errors.add("Referenced MCP tool has no identifier");
                    continue;
                }
                AbstractTool tool = toolDAO.findById(server.getId());
                if (tool == null) {
                    tool = globalToolDAO.findById(server.getId());
                }
                if (tool == null) {
                    errors.add("Referenced tool '" + server.getId() + "' does not exist");
                } else {
                    if (tool.getType() != ToolType.MCP) {
                        errors.add("Referenced tool '" + server.getId() + "' is no longer an MCP tool");
                    }
                    validateCredential(errors, "tool", server.getId(), server.getCredentialRef(),
                            tool.getAuthMode() == AuthMode.API_KEY, tool.getApiKey());
                }
            }
        }
        return errors;
    }

    private void validateCredential(List<String> errors, String kind, String id, String reference,
            boolean required, String apiKey) {
        var expected = "credential://" + kind + "/" + id + "/api-key";
        if ((required || reference != null) && !expected.equals(reference)) {
            errors.add("Invalid credential reference for " + kind + " '" + id + "'");
        }
        if (required && blank(apiKey)) {
            errors.add("Required credential for " + kind + " '" + id + "' is missing");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
