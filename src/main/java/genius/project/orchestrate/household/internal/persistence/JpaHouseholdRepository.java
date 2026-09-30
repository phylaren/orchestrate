package genius.project.orchestrate.household.internal.persistence;

import genius.project.orchestrate.household.internal.HouseholdRepository;
import genius.project.orchestrate.household.internal.domain.Household;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class JpaHouseholdRepository implements HouseholdRepository {

    private final SpringDataHouseholdRepository repository;


    public JpaHouseholdRepository(SpringDataHouseholdRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Household save(Household household) {
        HouseholdEntity entity = repository.findById(household.id())
                .map(existing -> {
                    existing.setName(household.name());
                    existing.setOwnerId(household.ownerId());
                    return existing;
                })
                .orElseGet(() -> new HouseholdEntity(
                        household.id(), household.name(), household.ownerId(), household.createdAt()));
        return HouseholdPersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Household> findById(UUID householdId) {
        return repository.findById(householdId).map(HouseholdPersistenceMapper::toDomain);
    }


    /** Memberships and their permissions go with the household via cascade; the invitation code via ON DELETE CASCADE. */
    @Override
    @Transactional
    public void deleteById(UUID householdId) {
        repository.findById(householdId).ifPresent(repository::delete);
    }
}
