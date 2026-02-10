package org.tkit.onecx.ai.provider.domain.criteria;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class ConfigurationSearchCriteria {

    private String name;

    private String description;

    private Integer pageNumber;

    private Integer pageSize;

}
