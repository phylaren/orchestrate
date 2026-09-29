package genius.project.orchestrate.chore.internal.persistence;

import genius.project.orchestrate.chore.internal.domain.*;

import java.util.List;

public final class ChoreEntityMapper {

    private ChoreEntityMapper() {}

    public static Chore toDomain(ChoreEntity entity) {
        if (entity == null) return null;
        return new Chore(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getName(),
                entity.getDescription(),
                entity.getRecurrenceDays(),
                entity.isRequiresConfirmation(),
                entity.getCreatedAt()
        );
    }

    public static ChoreEntity toEntity(Chore domain) {
        if (domain == null) return null;
        return new ChoreEntity(
                domain.id(),
                domain.householdId(),
                domain.name(),
                domain.description(),
                domain.recurrenceDays(),
                domain.requiresConfirmation(),
                domain.createdAt()
        );
    }


    public static void copyScalarsInto(Chore source, ChoreEntity target) {
        target.setHouseholdId(source.householdId());
        target.setName(source.name());
        target.setDescription(source.description());
        target.setRecurrenceDays(source.recurrenceDays());
        target.setRequiresConfirmation(source.requiresConfirmation());
    }


    public static ChoreWithParticipants toDomainWithParticipants(ChoreEntity entity) {
        List<ChoreParticipant> participants = entity.getParticipants().stream()
                .map(ChoreEntityMapper::toDomain)
                .toList();
        return new ChoreWithParticipants(toDomain(entity), participants);
    }

    public static ChoreParticipant toDomain(ChoreParticipantEntity entity) {
        if (entity == null) return null;
        return new ChoreParticipant(
                entity.getId().getChoreId(),
                entity.getId().getUserId(),
                entity.getJoinedAt(),
                entity.isAddedByAdmin()
        );
    }

    public static ChoreCompletion toDomain(ChoreCompletionEntity entity) {
        if (entity == null) return null;
        return new ChoreCompletion(
                entity.getId(),
                entity.getChore().getId(),
                entity.getCompletedByUserId(),
                entity.getCompletedAt(),
                entity.getStatus(),
                entity.getConfirmedByUserId(),
                entity.getConfirmedAt()
        );
    }

    public static RotationSchedule toDomain(RotationScheduleEntity entity) {
        if (entity == null) return null;
        return new RotationSchedule(
                entity.getChoreId(),
                entity.getBaseOrder(),
                entity.getCurrentIndex(),
                entity.getCurrentCycleNumber(),
                entity.getCycleStartedAt()
        );
    }

    public static RotationScheduleEntity toEntity(RotationSchedule domain) {
        if (domain == null) return null;
        return new RotationScheduleEntity(
                domain.choreId(),
                domain.baseOrder(),
                domain.currentIndex(),
                domain.currentCycleNumber(),
                domain.cycleStartedAt()
        );
    }

    public static ScheduledSwap toDomain(ScheduledSwapEntity entity) {
        if (entity == null) return null;
        return new ScheduledSwap(
                entity.getId(),
                entity.getChoreId(),
                entity.getFromUserId(),
                entity.getToUserId(),
                entity.getCycleNumber(),
                entity.getCreatedAt()
        );
    }

    public static ScheduledSwapEntity toEntity(ScheduledSwap domain) {
        if (domain == null) return null;
        return new ScheduledSwapEntity(
                domain.id(),
                domain.choreId(),
                domain.fromUserId(),
                domain.toUserId(),
                domain.cycleNumber(),
                domain.createdAt()
        );
    }
}
