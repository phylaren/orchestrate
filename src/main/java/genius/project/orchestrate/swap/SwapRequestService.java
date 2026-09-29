package genius.project.orchestrate.swap;

import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.dto.SwapRequestSummaryResponse;
import genius.project.orchestrate.swap.dto.SwapRequestUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface SwapRequestService {

    List<SwapRequestResponse> getSwapRequests(UUID choreId);

    List<SwapRequestResponse> getSwapRequests(UUID choreId, SwapRequestStatus status);

    SwapRequestResponse getSwapRequest(UUID choreId, UUID requestId);

    SwapRequestSummaryResponse getSummary(UUID choreId);

    SwapRequestResponse createSwapRequest(UUID choreId, SwapRequestRequest request);

    SwapRequestResponse updateSwapRequest(UUID choreId, UUID requestId, SwapRequestUpdateRequest request);

    SwapRequestResponse respondToSwapRequest(UUID choreId, UUID requestId, SwapRequestStatusRequest request);

    void deleteSwapRequest(UUID choreId, UUID requestId);
}
