package genius.project.orchestrate.swap;

import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.dto.SwapRequestSummaryResponse;
import genius.project.orchestrate.swap.dto.SwapRequestUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
            @PathVariable UUID choreId,
            @RequestParam(required = false) SwapRequestStatus status) {
        return ResponseEntity.ok(swapRequestService.getSwapRequests(choreId, status));
    }

    @GetMapping("/summary")
    public ResponseEntity<SwapRequestSummaryResponse> getSummary(@PathVariable UUID choreId) {
        return ResponseEntity.ok(swapRequestService.getSummary(choreId));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<SwapRequestResponse> getSwapRequest(
            @PathVariable UUID choreId,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(swapRequestService.getSwapRequest(choreId, requestId));
    }

    @PostMapping
    public ResponseEntity<SwapRequestResponse> createSwapRequest(
            @PathVariable UUID choreId,
            @Valid @RequestBody SwapRequestRequest request) {
        SwapRequestResponse created = swapRequestService.createSwapRequest(choreId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<SwapRequestResponse> updateSwapRequest(
            @PathVariable UUID choreId,
            @PathVariable UUID requestId,
            @Valid @RequestBody SwapRequestUpdateRequest request) {
        return ResponseEntity.ok(swapRequestService.updateSwapRequest(choreId, requestId, request));
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<SwapRequestResponse> respondToSwapRequest(
            @PathVariable UUID choreId,
            @PathVariable UUID requestId,
            @Valid @RequestBody SwapRequestStatusRequest request) {
        return ResponseEntity.ok(swapRequestService.respondToSwapRequest(choreId, requestId, request));
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteSwapRequest(
            @PathVariable UUID choreId,
            @PathVariable UUID requestId) {
        swapRequestService.deleteSwapRequest(choreId, requestId);
        return ResponseEntity.noContent().build();
    }
}
