package genius.project.orchestrate.chore.internal.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "scheduled_swaps")
public class ScheduledSwapEntity {

    @Id
    private UUID id;

    @Column(name = "chore_id", nullable = false)
    private UUID choreId;

    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScheduledSwapEntity() {}

    public ScheduledSwapEntity(UUID id, UUID choreId, UUID fromUserId, UUID toUserId,
                               int cycleNumber, Instant createdAt) {
        this.id = id;
        this.choreId = choreId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.cycleNumber = cycleNumber;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getChoreId() { return choreId; }
    public UUID getFromUserId() { return fromUserId; }
    public UUID getToUserId() { return toUserId; }
    public int getCycleNumber() { return cycleNumber; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledSwapEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}