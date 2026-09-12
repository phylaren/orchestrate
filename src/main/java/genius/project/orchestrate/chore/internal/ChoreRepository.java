package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreAssignment;
import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ChoreRepository {

    private final Map<UUID, Chore> chores = new HashMap<>();
    private final Map<UUID, LinkedHashMap<UUID, ChoreParticipant>> participantsByChore = new HashMap<>();
    private final Map<UUID, ChoreAssignment> assignments = new HashMap<>();
    private final Map<UUID, ChoreCompletion> completionIndex = new HashMap<>();

    public Chore saveChore(Chore chore) {
        chores.put(chore.id(), chore);
        participantsByChore.putIfAbsent(chore.id(), new LinkedHashMap<>());
        return chore;
    }

    public Optional<Chore> findChoreById(UUID choreId) {
        return Optional.ofNullable(chores.get(choreId));
    }

    public List<Chore> findAllChores() {
        return List.copyOf(chores.values());
    }

    public LinkedHashMap<UUID, ChoreParticipant> participantGroup(UUID choreId) {
        return participantsByChore.computeIfAbsent(choreId, k -> new LinkedHashMap<>());
    }

    public boolean isParticipant(UUID choreId, UUID userId) {
        return participantGroup(choreId).containsKey(userId);
    }

    public void swapParticipantOrder(UUID choreId, UUID userIdA, UUID userIdB) {
        LinkedHashMap<UUID, ChoreParticipant> group = participantGroup(choreId);
        List<UUID> order = new ArrayList<>(group.keySet());
        int indexA = order.indexOf(userIdA);
        int indexB = order.indexOf(userIdB);
        order.set(indexA, userIdB);
        order.set(indexB, userIdA);

        LinkedHashMap<UUID, ChoreParticipant> rebuilt = new LinkedHashMap<>();
        for (UUID userId : order) {
            rebuilt.put(userId, group.get(userId));
        }
        group.clear();
        group.putAll(rebuilt);
    }

    public Optional<ChoreAssignment> findAssignment(UUID choreId) {
        return Optional.ofNullable(assignments.get(choreId));
    }

    public ChoreAssignment saveAssignment(ChoreAssignment assignment) {
        assignments.put(assignment.choreId(), assignment);
        return assignment;
    }

    public ChoreCompletion saveCompletion(ChoreCompletion completion) {
        completionIndex.put(completion.id(), completion);
        return completion;
    }

    public Optional<ChoreCompletion> findCompletionById(UUID completionId) {
        return Optional.ofNullable(completionIndex.get(completionId));
    }

    public List<ChoreCompletion> findCompletionsByChoreId(UUID choreId) {
        return completionIndex.values().stream()
                .filter(c -> c.choreId().equals(choreId))
                .toList();
    }
}
