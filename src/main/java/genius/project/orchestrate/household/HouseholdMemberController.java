package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.MembershipResponse;
import genius.project.orchestrate.household.dto.PermissionsUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/members")
public class HouseholdMemberController {

    private final HouseholdService householdService;

    public HouseholdMemberController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping
    public List<MembershipResponse> list(@PathVariable UUID householdId) {
        return householdService.listMembers(householdId).stream()
                .map(MembershipResponse::from)
                .toList();
    }

    @GetMapping("/{userId}")
    public MembershipResponse get(@PathVariable UUID householdId, @PathVariable UUID userId) {
        return MembershipResponse.from(householdService.getMember(householdId, userId));
    }

    /** Removes another member, or leaves the household when {@code userId} is the current user. */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable UUID householdId, @PathVariable UUID userId) {
        householdService.removeMember(householdId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/permissions")
    public MembershipResponse updatePermissions(@PathVariable UUID householdId,
                                                @PathVariable UUID userId,
                                                @Valid @RequestBody PermissionsUpdateRequest request) {
        return MembershipResponse.from(
                householdService.updatePermissions(householdId, userId, request.permissions()));
    }
}
