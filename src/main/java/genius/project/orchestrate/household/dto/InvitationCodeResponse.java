package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.internal.domain.InvitationCode;

import java.time.Instant;
import java.util.UUID;

public record InvitationCodeResponse(
        String code,
        UUID householdId,
        UUID createdByUserId,
        Instant createdAt,
        Instant expiresAt
) {
    public static InvitationCodeResponse from(InvitationCode code) {
        return new InvitationCodeResponse(
                code.code(), code.householdId(), code.createdByUserId(), code.createdAt(), code.expiresAt());
    }
}
