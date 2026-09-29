package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreWithParticipants;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
class ChoreLifecycleServiceImpl implements ChoreLifecycleService {

    private final ChoreStore choreStore;

    ChoreLifecycleServiceImpl(ChoreStore choreStore) {
        this.choreStore = choreStore;
    }

    @Override
    public ChoreResponse createChore(UUID householdId, String name, String description,
                                     int recurrenceDays, boolean requiresConfirmation) {
        Chore chore = new Chore(
                UUID.randomUUID(), householdId, name, description,
                recurrenceDays, requiresConfirmation, Instant.now());
        Chore saved = choreStore.save(chore);
        return ChoreResponse.from(saved, true);
    }

    @Override
    public List<ChoreResponse> listChores(UUID householdId) {
        return choreStore.findAllWithParticipants(householdId).stream()
                .sorted(Comparator.comparing(c -> c.chore().createdAt()))
                .map(c -> ChoreResponse.from(c.chore(), c.needsAttention()))
                .toList();
    }

    @Override
    public ChoreResponse getChore(UUID choreId) {
        ChoreWithParticipants found = getOrThrow(choreId);
        return ChoreResponse.from(found.chore(), found.needsAttention());
    }

    @Override
    public ChoreResponse updateChore(UUID choreId, String name, String description,
                                     int recurrenceDays, boolean requiresConfirmation) {
        ChoreWithParticipants found = getOrThrow(choreId);
        Chore current = found.chore();

        Chore updated = new Chore(
                current.id(), current.householdId(), name, description,
                recurrenceDays, requiresConfirmation, current.createdAt());
        Chore saved = choreStore.save(updated);
        return ChoreResponse.from(saved, found.needsAttention());
    }

    @Override
    public void deleteChore(UUID choreId) {
        if (choreStore.findById(choreId).isEmpty()) {
            throw ResourceNotFoundException.of("chore", choreId);
        }
        choreStore.deleteById(choreId);
    }

    private ChoreWithParticipants getOrThrow(UUID choreId) {
        return choreStore.findByIdWithParticipants(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
    }
}