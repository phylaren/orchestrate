package genius.project.orchestrate.swap.internal.persistence;

import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SwapRequestPersistenceMapper {

    SwapRequest toDomain(SwapRequestEntity entity);

    SwapRequestEntity toEntity(SwapRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "choreId", ignore = true)
    @Mapping(target = "initiatorUserId", ignore = true)
    @Mapping(target = "receiverUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void applyMutableTerms(SwapRequest request, @MappingTarget SwapRequestEntity entity);
}
