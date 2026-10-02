package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreWithParticipants;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
class ChoreLifecycleServiceImpl implements ChoreLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ChoreLifecycleServiceImpl.class);

    private final ChoreStore choreStore;
    private final CurrentUserProvider currentUserProvider;

    ChoreLifecycleServiceImpl(ChoreStore choreStore,
                              CurrentUserProvider currentUserProvider) {
        this.choreStore = choreStore;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public ChoreResponse createChore(UUID householdId, String name, String description,
                                     int recurrenceDays, boolean requiresConfirmation) {
        UUID actorId = currentUserProvider.getUserId();
        Chore chore = new Chore(
                UUID.randomUUID(), householdId, name, description,
                recurrenceDays, requiresConfirmation, Instant.now());
        Chore saved = choreStore.save(chore);
        log.info("Chore created: choreId={}, householdId={}, by={}",
                saved.id(), saved.householdId(), actorId);
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
        UUID actorId = currentUserProvider.getUserId();
        Chore current = found.chore();

        Chore updated = new Chore(
                current.id(), current.householdId(), name, description,
                recurrenceDays, requiresConfirmation, current.createdAt());
        Chore saved = choreStore.save(updated);
        log.info("Chore updated: choreId={}, householdId={}, by={}",
                saved.id(), saved.householdId(), actorId);
        return ChoreResponse.from(saved, found.needsAttention());
    }

    @Override
    public void deleteChore(UUID choreId) {
        if (choreStore.findById(choreId).isEmpty()) {
            throw ResourceNotFoundException.of("chore", choreId);
        }
        UUID actorId = currentUserProvider.getUserId();
        choreStore.deleteById(choreId);
        log.info("Chore deleted: choreId={}, by={}", choreId, actorId);
    }

    private ChoreWithParticipants getOrThrow(UUID choreId) {
        return choreStore.findByIdWithParticipants(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
    }
}