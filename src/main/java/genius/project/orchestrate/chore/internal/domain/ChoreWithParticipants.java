package genius.project.orchestrate.chore.internal.domain;

import java.util.List;
import java.util.Objects;

public record ChoreWithParticipants(Chore chore, List<ChoreParticipant> participants) {

    public ChoreWithParticipants {
        Objects.requireNonNull(chore, "chore");
        participants = List.copyOf(participants);
    }

    public boolean needsAttention() {
        return participants.isEmpty();
    }
}
