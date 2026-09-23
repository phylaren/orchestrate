package genius.project.orchestrate.chore.internal.service;

import genius.project.orchestrate.chore.ChoreParticipantService;
import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.ParticipantResponse;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;
import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.repository.ChoreParticipantStore;
import genius.project.orchestrate.chore.internal.repository.ChoreStore;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ChoreParticipantServiceImpl implements ChoreParticipantService, ChoreClient {

    private final ChoreStore choreStore;
    private final ChoreParticipantStore participantStore;
    private final RotationService rotationService;

    ChoreParticipantServiceImpl(ChoreStore choreStore,
                                ChoreParticipantStore participantStore,
                                RotationService rotationService) {
        this.choreStore = choreStore;
        this.participantStore = participantStore;
        this.rotationService = rotationService;
    }

    @Override
    public ParticipantResponse joinChore(UUID choreId, UUID userId, boolean addedByAdmin) {
        getChoreOrThrow(choreId);

        if (participantStore.existsByChoreIdAndUserId(choreId, userId)) {
            throw new BusinessRuleViolationException(
                    "ALREADY_PARTICIPANT",
                    "User '%s' has already joined the rotation group for this chore.".formatted(userId));
        }

        ChoreParticipant participant = new ChoreParticipant(choreId, userId, Instant.now(), addedByAdmin);
        participantStore.save(participant);
        rotationService.addParticipant(choreId, userId);

        return ParticipantResponse.from(participant);
    }

    @Override
    public void leaveChore(UUID choreId, UUID userId) {
        getChoreOrThrow(choreId);

        if (!participantStore.existsByChoreIdAndUserId(choreId, userId)) {
            throw ResourceNotFoundException.of("participant", userId);
        }

        participantStore.deleteByChoreIdAndUserId(choreId, userId);
        rotationService.removeParticipant(choreId, userId);
    }

    @Override
    public List<ParticipantResponse> listParticipants(UUID choreId) {
        getChoreOrThrow(choreId);
        return participantStore.findByChoreId(choreId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }

    @Override
    public boolean isParticipant(UUID choreId, UUID userId) {
        getChoreOrThrow(choreId);
        return participantStore.existsByChoreIdAndUserId(choreId, userId);
    }

    @Override
    public int currentCycleNumber(UUID choreId) {
        getChoreOrThrow(choreId);
        return rotationService.getSchedule(choreId)
                .map(RotationScheduleResponse::cycleNumber)
                .orElseThrow(() -> ResourceNotFoundException.of("rotationSchedule", choreId));
    }

    @Override
    public Optional<AssignmentResponse> getCurrentAssignment(UUID choreId) {
        getChoreOrThrow(choreId);
        return rotationService.getCurrentAssignment(choreId);
    }

    private Chore getChoreOrThrow(UUID choreId) {
        return choreStore.findById(choreId)
                .orElseThrow(() -> ResourceNotFoundException.of("chore", choreId));
    }
}