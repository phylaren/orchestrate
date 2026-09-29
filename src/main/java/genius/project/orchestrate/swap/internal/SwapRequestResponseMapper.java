package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SwapRequestResponseMapper {

    SwapRequestResponse toResponse(SwapRequest request);
}
