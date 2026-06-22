package org.tkit.onecx.ai.provider.domain.criteria;

import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class ToolSearchCriteria {

    private String name;

    private String description;

    private String url;

    private ToolType type;

    private Integer pageNumber;

    private Integer pageSize;
}
