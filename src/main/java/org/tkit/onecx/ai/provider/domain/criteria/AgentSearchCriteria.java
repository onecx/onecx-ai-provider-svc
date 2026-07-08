package org.tkit.onecx.ai.provider.domain.criteria;

import org.tkit.onecx.ai.provider.domain.models.enums.AgentStatus;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class AgentSearchCriteria {

    private String name;

    private String description;

    private AgentStatus status;

    private Integer pageNumber;

    private Integer pageSize;
}
