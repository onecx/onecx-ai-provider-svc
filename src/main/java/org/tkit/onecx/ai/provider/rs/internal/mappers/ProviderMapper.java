package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.ProviderSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ProviderMapper {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Provider createProvider(CreateProviderRequestDTO aiProviderDTO);

    ProviderDTO map(Provider provider);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Provider map(ProviderDTO provider);

    ProviderSearchCriteria mapCriteria(ProviderSearchCriteriaDTO aiProviderSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    ProviderPageResultDTO mapPageResult(PageResult<Provider> result);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void update(UpdateProviderRequestDTO aiProviderDTO, @MappingTarget Provider provider);
}
