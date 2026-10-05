package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.internal.domain.InvitationCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Код запрошення, за яким новий мешканець приєднується до домогосподарства")
public record InvitationCodeResponse(
        @Schema(description = "Значення коду (регістр не враховується)", example = "K7M2QX9P")
        String code,

        @Schema(description = "Домогосподарство, до якого веде код", example = "9b2f6c3e-1a4d-4e5b-8f7a-0c1d2e3f4a5b")
        UUID householdId,

        @Schema(description = "Користувач, що створив код", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID createdByUserId,

        @Schema(description = "Момент створення", example = "2026-10-05T10:15:30Z")
        Instant createdAt,

        @Schema(description = "Момент, після якого код недійсний (7 днів від створення)",
                example = "2026-10-12T10:15:30Z")
        Instant expiresAt
) {
    public static InvitationCodeResponse from(InvitationCode code) {
        return new InvitationCodeResponse(
                code.code(), code.householdId(), code.createdByUserId(), code.createdAt(), code.expiresAt());
    }
}
