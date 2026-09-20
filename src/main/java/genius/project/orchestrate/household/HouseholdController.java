package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.HouseholdCreateRequest;
import genius.project.orchestrate.household.dto.HouseholdResponse;
import genius.project.orchestrate.household.dto.OwnershipTransferRequest;
import genius.project.orchestrate.household.internal.domain.Household;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    public ResponseEntity<HouseholdResponse> createHousehold(@Valid @RequestBody HouseholdCreateRequest request,
                                                               UriComponentsBuilder uriBuilder) {
        Household household = householdService.createHousehold(request.name());
        URI location = uriBuilder.path("/api/v1/households/{id}").buildAndExpand(household.id()).toUri();
        return ResponseEntity.created(location).body(HouseholdResponse.from(household));
    }

    @GetMapping("/{householdId}")
    public HouseholdResponse getHousehold(@PathVariable UUID householdId) {
        return HouseholdResponse.from(householdService.getHousehold(householdId));
    }

    @DeleteMapping("/{householdId}")
    public ResponseEntity<Void> deleteHousehold(@PathVariable UUID householdId) {
        householdService.deleteHousehold(householdId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{householdId}/ownership-transfer")
    public HouseholdResponse transferOwnership(@PathVariable UUID householdId,
                                               @Valid @RequestBody OwnershipTransferRequest request) {
        return HouseholdResponse.from(householdService.transferOwnership(householdId, request.newOwnerUserId()));
    }
}
