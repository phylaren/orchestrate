package genius.project.orchestrate.household.internal.persistence;

import genius.project.orchestrate.household.internal.MembershipRepository;
import genius.project.orchestrate.household.internal.domain.Membership;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class JpaMembershipRepository implements MembershipRepository {

    private final SpringDataMembershipRepository membershipRepository;
    private final SpringDataHouseholdRepository householdRepository;

    public JpaMembershipRepository(SpringDataMembershipRepository membershipRepository,
                                   SpringDataHouseholdRepository householdRepository) {
        this.membershipRepository = membershipRepository;
        this.householdRepository = householdRepository;
    }


    @Override
    @Transactional
    public Membership save(Membership membership) {
        MembershipEntity entity = membershipRepository
                .findWithPermissions(membership.householdId(), membership.userId())
                .map(existing -> {
                    existing.replacePermissions(membership.permissions());
                    return existing;
                })
                .orElseGet(() -> newMembership(membership));
        // No repository.save(): a new membership is persisted by the household's cascade, an existing one
        // by dirty checking. save() would merge an entity with an assigned id into a second managed copy.
        return HouseholdPersistenceMapper.toDomain(entity);
    }

    // Keeps both sides of Household 1:N Membership in sync so cascade and orphanRemoval see the new row.
    private MembershipEntity newMembership(Membership membership) {
        HouseholdEntity household = householdRepository.getReferenceById(membership.householdId());
        MembershipEntity entity = new MembershipEntity(
                household,
                new MembershipId(membership.householdId(), membership.userId()),
                membership.permissions(),
                membership.joinedAt());
        household.getMemberships().add(entity);
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Membership> find(UUID householdId, UUID userId) {
        return membershipRepository.findWithPermissions(householdId, userId)
                .map(HouseholdPersistenceMapper::toDomain);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Membership> findByHouseholdId(UUID householdId) {
        return membershipRepository.findAllByHouseholdIdWithPermissions(householdId).stream()
                .map(HouseholdPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Membership> findByUserId(UUID userId) {
        return membershipRepository.findAllByUserIdWithPermissions(userId).stream()
                .map(HouseholdPersistenceMapper::toDomain)
                .toList();
    }

    /** Removing the membership from the household's collection lets orphanRemoval delete it together with its permissions. */
    @Override
    @Transactional
    public void delete(UUID householdId, UUID userId) {
        householdRepository.findById(householdId).ifPresent(household ->
                household.getMemberships().removeIf(m -> m.getId().getUserId().equals(userId)));
    }

    @Override
    @Transactional
    public void deleteByHouseholdId(UUID householdId) {
        householdRepository.findById(householdId).ifPresent(household -> household.getMemberships().clear());
    }
}
