package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.ScheduledSwap;
import genius.project.orchestrate.chore.internal.persistence.ChoreEntityMapper;
import genius.project.orchestrate.chore.internal.repository.jpa.SpringDataScheduledSwapRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Primary
public class JpaScheduledSwapRepository implements ScheduledSwapRepository {

    private final SpringDataScheduledSwapRepository repository;

    public JpaScheduledSwapRepository(SpringDataScheduledSwapRepository repository) {
        this.repository = repository;
    }

    @Override
    public ScheduledSwap save(ScheduledSwap swap) {
        var entity = ChoreEntityMapper.toEntity(swap);
        var saved = repository.save(entity);
        return ChoreEntityMapper.toDomain(saved);
    }

    @Override
    public List<ScheduledSwap> findByChoreIdAndCycleNumber(UUID choreId, int cycleNumber) {
        return repository.findByChoreIdAndCycleNumber(choreId, cycleNumber).stream()
                .map(ChoreEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteExpired(UUID choreId, int currentCycleNumber) {
        repository.deleteExpired(choreId, currentCycleNumber);
    }
}