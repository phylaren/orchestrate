package genius.project.orchestrate.swap;

import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;

import java.util.List;

public interface SwapRequestService {
    List<SwapRequestResponse> getSwapRequests(Long choreId);
    SwapRequestResponse createSwapRequest(Long choreId, SwapRequestRequest request);
    SwapRequestResponse respondToSwapRequest(Long choreId, Long requestId, SwapRequestStatusRequest request);
}