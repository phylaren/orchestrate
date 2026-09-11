package genius.project.orchestrate.swap;

import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chores/{choreId}/swap-requests")
public class SwapRequestController {

    private final SwapRequestService swapRequestService;

    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    // TODO: add access check — current user must belong to the household that owns
    //  this chore. Pending the authorization layer and a public port from the
    //  household module to resolve household membership by choreId.
    @GetMapping
    public ResponseEntity<List<SwapRequestResponse>> getSwapRequests(
            @PathVariable Long choreId) {
        return ResponseEntity.ok(swapRequestService.getSwapRequests(choreId));
    }

    @PostMapping
    public ResponseEntity<SwapRequestResponse> createSwapRequest(
            @PathVariable Long choreId,
            @Valid @RequestBody SwapRequestRequest request) {
        SwapRequestResponse created = swapRequestService.createSwapRequest(choreId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<SwapRequestResponse> respondToSwapRequest(
            @PathVariable Long choreId,
            @PathVariable Long requestId,
            @Valid @RequestBody SwapRequestStatusRequest request) {
        return ResponseEntity.ok(swapRequestService.respondToSwapRequest(choreId, requestId, request));
    }
}