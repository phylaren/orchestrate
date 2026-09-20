package genius.project.orchestrate.chore.internal.repository.inmemory;

import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import genius.project.orchestrate.chore.internal.repository.RotationRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryRotationRepository implements RotationRepository {

    private final Map<UUID, RotationSchedule> store = new ConcurrentHashMap<>();

    @Override
    public RotationSchedule save(RotationSchedule schedule) {
        store.put(schedule.choreId(), schedule);
        return schedule;
    }

    @Override
    public Optional<RotationSchedule> findByChoreId(UUID choreId) {
        return Optional.ofNullable(store.get(choreId));
    }
}
