package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.persistence.ChoreEntityMapper;
import genius.project.orchestrate.chore.internal.repository.jpa.SpringDataRotationRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@Primary
public class JpaRotationRepository implements RotationRepository {

    private final SpringDataRotationRepository repository;

    public JpaRotationRepository(SpringDataRotationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RotationSchedule save(RotationSchedule schedule) {
        var entity = ChoreEntityMapper.toEntity(schedule);
        var saved = repository.save(entity);
        return ChoreEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RotationSchedule> findByChoreId(UUID choreId) {
        return repository.findById(choreId).map(ChoreEntityMapper::toDomain);
    }
}