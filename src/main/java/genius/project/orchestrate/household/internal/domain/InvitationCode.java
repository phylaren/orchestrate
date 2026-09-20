package genius.project.orchestrate.household.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record InvitationCode(
    String code,
    UUID householdId,
    UUID createdByUserId,
    Instant createdAt,
    Instant expiresAt
) {
    public boolean isExpiredAt(Instant moment) {
        return !moment.isBefore(expiresAt);
    }
}
