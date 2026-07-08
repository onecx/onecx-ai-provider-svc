package org.tkit.onecx.ai.provider.domain.criteria;

import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupOrchestrationMode;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class AgentGroupSearchCriteria {

    private String name;

    private AgentGroupOrchestrationMode orchestrationMode;

    private Integer pageNumber;

    private Integer pageSize;
}
