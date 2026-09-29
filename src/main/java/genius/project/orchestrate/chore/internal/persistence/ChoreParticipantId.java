package genius.project.orchestrate.chore.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ChoreParticipantId implements Serializable {

    @Column(name = "chore_id")
    private UUID choreId;

    @Column(name = "user_id")
    private UUID userId;

    public ChoreParticipantId() {}

    public ChoreParticipantId(UUID choreId, UUID userId) {
        this.choreId = choreId;
        this.userId = userId;
    }

    public UUID getChoreId() { return choreId; }
    public UUID getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoreParticipantId that)) return false;
        return Objects.equals(choreId, that.choreId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(choreId, userId);
    }
}