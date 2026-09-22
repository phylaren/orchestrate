package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ChoreLifecycleService;
import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.dto.ChoreResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
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
    private final RotationService rotationService;

    ChoreLifecycleServiceImpl(ChoreStore choreStore, RotationService rotationService) {
        this.choreStore = choreStore;
        this.rotationService = rotationService;
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
        return choreStore.findAll().stream()
                .filter(c -> householdId == null || householdId.equals(c.householdId()))
                .sorted(Comparator.comparing(Chore::createdAt))
                .map(c -> ChoreResponse.from(c, needsAttention(c.id())))
                .toList();
    }

    @Override
    public ChoreResponse getChore(UUID choreId) {
        Chore chore = choreStore.findById(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
        return ChoreResponse.from(chore, needsAttention(choreId));
    }

    private boolean needsAttention(UUID choreId) {
        return rotationService.isEmpty(choreId);
    }
}
