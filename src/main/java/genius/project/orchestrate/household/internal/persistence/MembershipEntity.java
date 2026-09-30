package genius.project.orchestrate.household.internal.persistence;

import genius.project.orchestrate.household.MembershipPermission;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "memberships")
public class MembershipEntity {

    @EmbeddedId
    private MembershipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("householdId")
    @JoinColumn(name = "household_id")
    private HouseholdEntity household;


    @ElementCollection
    @CollectionTable(
            name = "membership_permissions",
            joinColumns = {
                    @JoinColumn(name = "household_id", referencedColumnName = "household_id"),
                    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
            })
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 50)
    private Set<MembershipPermission> permissions = new HashSet<>();

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected MembershipEntity() {}

    public MembershipEntity(HouseholdEntity household, MembershipId id,
                            Set<MembershipPermission> permissions, Instant joinedAt) {
        this.household = household;
        this.id = id;
        this.permissions = new HashSet<>(permissions);
        this.joinedAt = joinedAt;
    }

    public MembershipId getId() { return id; }
    public HouseholdEntity getHousehold() { return household; }
    public Instant getJoinedAt() { return joinedAt; }

    public Set<MembershipPermission> getPermissions() {
        return permissions.isEmpty() ? EnumSet.noneOf(MembershipPermission.class) : EnumSet.copyOf(permissions);
    }

    public void replacePermissions(Set<MembershipPermission> newPermissions) {
        permissions.clear();
        permissions.addAll(newPermissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
