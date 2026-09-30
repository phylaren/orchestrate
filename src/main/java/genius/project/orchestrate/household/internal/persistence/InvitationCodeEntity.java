package genius.project.orchestrate.household.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitation_codes")
public class InvitationCodeEntity {

    @Id
    @Column(length = 32)
    private String code;


    // Unidirectional on purpose: a mappedBy side on HouseholdEntity could not be lazy
    // and would add one query per loaded household.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, unique = true, updatable = false)
    private HouseholdEntity household;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    protected InvitationCodeEntity() {}

    public InvitationCodeEntity(String code, HouseholdEntity household, UUID createdByUserId,
                                Instant createdAt, Instant expiresAt) {
        this.code = code;
        this.household = household;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }


    public String getCode() { return code; }
    public HouseholdEntity getHousehold() { return household; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvitationCodeEntity that)) return false;
        return code != null && code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
