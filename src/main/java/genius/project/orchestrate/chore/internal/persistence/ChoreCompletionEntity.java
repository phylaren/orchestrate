package genius.project.orchestrate.chore.internal.persistence;

import genius.project.orchestrate.chore.ConfirmationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chore_completions")
public class ChoreCompletionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chore_id", nullable = false)
    private ChoreEntity chore;

    @Column(name = "completed_by_user_id", nullable = false)
    private UUID completedByUserId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfirmationStatus status;

    @Column(name = "confirmed_by_user_id")
    private UUID confirmedByUserId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected ChoreCompletionEntity() {}

    public ChoreCompletionEntity(UUID id, ChoreEntity chore, UUID completedByUserId, Instant completedAt,
                                 ConfirmationStatus status, UUID confirmedByUserId, Instant confirmedAt) {
        this.id = id;
        this.chore = chore;
        this.completedByUserId = completedByUserId;
        this.completedAt = completedAt;
        this.status = status;
        this.confirmedByUserId = confirmedByUserId;
        this.confirmedAt = confirmedAt;
    }

    public UUID getId() { return id; }
    public ChoreEntity getChore() { return chore; }
    public void setChore(ChoreEntity chore) { this.chore = chore; }
    public UUID getCompletedByUserId() { return completedByUserId; }
    public Instant getCompletedAt() { return completedAt; }
    public ConfirmationStatus getStatus() { return status; }
    public void setStatus(ConfirmationStatus status) { this.status = status; }
    public UUID getConfirmedByUserId() { return confirmedByUserId; }
    public void setConfirmedByUserId(UUID confirmedByUserId) { this.confirmedByUserId = confirmedByUserId; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoreCompletionEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}