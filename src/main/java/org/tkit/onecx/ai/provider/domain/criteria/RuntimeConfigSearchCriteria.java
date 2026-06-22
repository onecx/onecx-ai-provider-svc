package org.tkit.onecx.ai.provider.domain.criteria;

import org.tkit.onecx.ai.provider.domain.models.enums.RuntimeConfigType;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class RuntimeConfigSearchCriteria {

    private String endpoint;

    private String providerId;

    private RuntimeConfigType type;

    private Integer pageNumber;

    private Integer pageSize;
}
