package genius.project.orchestrate.swap;

import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;

import java.util.List;
import java.util.UUID;

public interface SwapRequestService {
    List<SwapRequestResponse> getSwapRequests(UUID choreId);
    SwapRequestResponse createSwapRequest(UUID choreId, SwapRequestRequest request);
    SwapRequestResponse respondToSwapRequest(UUID choreId, UUID requestId, SwapRequestStatusRequest request);
}