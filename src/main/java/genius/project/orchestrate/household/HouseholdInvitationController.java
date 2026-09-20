package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.InvitationCodeResponse;
import genius.project.orchestrate.household.dto.JoinHouseholdRequest;
import genius.project.orchestrate.household.dto.MembershipResponse;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;
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
public class HouseholdInvitationController {

    private final HouseholdService householdService;

    public HouseholdInvitationController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping("/{householdId}/invitation-code")
    public ResponseEntity<InvitationCodeResponse> createInvitationCode(@PathVariable UUID householdId,
                                                                         UriComponentsBuilder uriBuilder) {
        InvitationCode code = householdService.createInvitationCode(householdId);
        URI location = uriBuilder.path("/api/v1/households/{householdId}/invitation-code")
                .buildAndExpand(householdId)
                .toUri();
        return ResponseEntity.created(location).body(InvitationCodeResponse.from(code));
    }

    @GetMapping("/{householdId}/invitation-code")
    public InvitationCodeResponse getInvitationCode(@PathVariable UUID householdId) {
        return InvitationCodeResponse.from(householdService.getInvitationCode(householdId));
    }

    @DeleteMapping("/{householdId}/invitation-code")
    public ResponseEntity<Void> revokeInvitationCode(@PathVariable UUID householdId) {
        householdService.revokeInvitationCode(householdId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/join")
    public ResponseEntity<MembershipResponse> join(@Valid @RequestBody JoinHouseholdRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        Membership membership = householdService.joinByInvitationCode(request.code());
        URI location = uriBuilder.path("/api/v1/households/{householdId}/members/{userId}")
                .buildAndExpand(membership.householdId(), membership.userId())
                .toUri();
        return ResponseEntity.created(location).body(MembershipResponse.from(membership));
    }
}
