package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.AssignmentResponse;
import genius.project.orchestrate.chore.dto.RotationScheduleResponse;

import java.util.Optional;
import java.util.UUID;

public interface RotationService {

    Optional<RotationScheduleResponse> getSchedule(UUID choreId);

    Optional<AssignmentResponse> getCurrentAssignment(UUID choreId);

    RotationScheduleResponse addParticipant(UUID choreId, UUID userId);

    RotationScheduleResponse removeParticipant(UUID choreId, UUID userId);

    RotationScheduleResponse advance(UUID choreId);

    void swap(UUID choreId, UUID fromUserId, UUID toUserId, SwapType swapType, Integer cycleNumber);

    Optional<UUID> currentResponsible(UUID choreId);

    boolean isEmpty(UUID choreId);
}