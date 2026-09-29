package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.ChoreParticipant;
import genius.project.orchestrate.chore.internal.persistence.ChoreEntityMapper;
import genius.project.orchestrate.chore.internal.persistence.ChoreParticipantEntity;
import genius.project.orchestrate.chore.internal.persistence.ChoreParticipantId;
import genius.project.orchestrate.chore.internal.repository.jpa.SpringDataChoreParticipantRepository;
import genius.project.orchestrate.chore.internal.repository.jpa.SpringDataChoreRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Primary
public class JpaChoreParticipantStore implements ChoreParticipantStore {

    private final SpringDataChoreParticipantRepository participantRepository;
    private final SpringDataChoreRepository choreRepository;

    public JpaChoreParticipantStore(SpringDataChoreParticipantRepository participantRepository,
                                    SpringDataChoreRepository choreRepository) {
        this.participantRepository = participantRepository;
        this.choreRepository = choreRepository;
    }

    @Override
    public ChoreParticipant save(ChoreParticipant participant) {
        var choreProxy = choreRepository.getReferenceById(participant.choreId());
        var id = new ChoreParticipantId(participant.choreId(), participant.userId());
        var entity = new ChoreParticipantEntity(
                choreProxy,
                id,
                participant.joinedAt(),
                participant.addedByAdmin()
        );
        var saved = participantRepository.save(entity);
        return ChoreEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<ChoreParticipant> findByChoreIdAndUserId(UUID choreId, UUID userId) {
        return participantRepository.findById(new ChoreParticipantId(choreId, userId))
                .map(ChoreEntityMapper::toDomain);
    }

    @Override
    public List<ChoreParticipant> findByChoreId(UUID choreId) {
        return participantRepository.findByIdChoreId(choreId).stream()
                .map(ChoreEntityMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByChoreIdAndUserId(UUID choreId, UUID userId) {
        return participantRepository.existsByIdChoreIdAndIdUserId(choreId, userId);
    }

    @Override
    @Transactional
    public void deleteByChoreIdAndUserId(UUID choreId, UUID userId) {
        participantRepository.deleteByIdChoreIdAndIdUserId(choreId, userId);
    }
}