package genius.project.orchestrate.household;

import genius.project.orchestrate.household.dto.InvitationCodeResponse;
import genius.project.orchestrate.household.dto.JoinHouseholdRequest;
import genius.project.orchestrate.household.dto.MembershipResponse;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Запрошення", description = "Код запрошення та приєднання нових мешканців до домогосподарства")
@RestController
@RequestMapping("/api/v1/households")
public class HouseholdInvitationController {

    private final HouseholdService householdService;

    public HouseholdInvitationController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @Operation(summary = "Створити код запрошення",
            description = "Потрібне право INVITE_MEMBERS (або статус власника). Код дійсний 7 днів.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Код створено",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InvitationCodeResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостатньо прав (MISSING_PERMISSION / NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство не знайдено (HOUSEHOLD_NOT_FOUND)")
    })
    @PostMapping("/{householdId}/invitation-code")
    public ResponseEntity<InvitationCodeResponse> createInvitationCode(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                                                         @PathVariable UUID householdId,
                                                                         UriComponentsBuilder uriBuilder) {
        InvitationCode code = householdService.createInvitationCode(householdId);
        URI location = uriBuilder.path("/api/v1/households/{householdId}/invitation-code")
                .buildAndExpand(householdId)
                .toUri();
        return ResponseEntity.created(location).body(InvitationCodeResponse.from(code));
    }

    @Operation(summary = "Отримати чинний код запрошення", description = "Потрібне право INVITE_MEMBERS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Чинний код запрошення"),
            @ApiResponse(responseCode = "403", description = "Недостатньо прав (MISSING_PERMISSION / NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство або чинний код не знайдено")
    })
    @GetMapping("/{householdId}/invitation-code")
    public InvitationCodeResponse getInvitationCode(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                                    @PathVariable UUID householdId) {
        return InvitationCodeResponse.from(householdService.getInvitationCode(householdId));
    }

    @Operation(summary = "Відкликати код запрошення", description = "Потрібне право INVITE_MEMBERS.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Код відкликано"),
            @ApiResponse(responseCode = "403", description = "Недостатньо прав (MISSING_PERMISSION / NOT_HOUSEHOLD_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "Домогосподарство або код не знайдено")
    })
    @DeleteMapping("/{householdId}/invitation-code")
    public ResponseEntity<Void> revokeInvitationCode(@Parameter(description = "Ідентифікатор домогосподарства", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
                                                     @PathVariable UUID householdId) {
        householdService.revokeInvitationCode(householdId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Приєднатися до домогосподарства за кодом",
            description = "Поточний користувач стає мешканцем з базовим набором прав (CONFIRM_COMPLETIONS).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Користувач приєднався",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MembershipResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "404", description = "Код запрошення не існує (INVITATION_CODE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Код прострочено або користувач уже учасник "
                    + "(INVITATION_CODE_EXPIRED / ALREADY_HOUSEHOLD_MEMBER)")
    })
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
