package genius.project.orchestrate.chore.internal.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "chore_participants")
public class ChoreParticipantEntity {

    @EmbeddedId
    private ChoreParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("choreId")
    @JoinColumn(name = "chore_id")
    private ChoreEntity chore;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "added_by_admin", nullable = false)
    private boolean addedByAdmin;

    protected ChoreParticipantEntity() {}

    public ChoreParticipantEntity(ChoreEntity chore, ChoreParticipantId id, Instant joinedAt, boolean addedByAdmin) {
        this.chore = chore;
        this.id = id;
        this.joinedAt = joinedAt;
        this.addedByAdmin = addedByAdmin;
    }

    public ChoreParticipantId getId() { return id; }
    public ChoreEntity getChore() { return chore; }
    public void setChore(ChoreEntity chore) { this.chore = chore; }
    public Instant getJoinedAt() { return joinedAt; }
    public boolean isAddedByAdmin() { return addedByAdmin; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoreParticipantEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}