package org.tkit.onecx.ai.provider.domain.criteria;

import org.tkit.onecx.ai.provider.domain.models.enums.CommunicationMode;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class ModelSearchCriteria {

    private String providerId;

    private String name;

    private CommunicationMode communicationMode;

    private Integer pageNumber;

    private Integer pageSize;
}
