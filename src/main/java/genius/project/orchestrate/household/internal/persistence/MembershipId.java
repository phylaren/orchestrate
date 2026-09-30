package genius.project.orchestrate.household.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MembershipId implements Serializable {

    @Column(name = "household_id")
    private UUID householdId;

    @Column(name = "user_id")
    private UUID userId;


    protected MembershipId() {}

    public MembershipId(UUID householdId, UUID userId) {
        this.householdId = householdId;
        this.userId = userId;
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipId that)) return false;
        return Objects.equals(householdId, that.householdId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(householdId, userId);
    }
}
