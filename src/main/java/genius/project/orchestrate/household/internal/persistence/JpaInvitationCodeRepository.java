package genius.project.orchestrate.household.internal.persistence;

import genius.project.orchestrate.household.internal.InvitationCodeRepository;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class JpaInvitationCodeRepository implements InvitationCodeRepository {

    private final SpringDataInvitationCodeRepository codeRepository;
    private final SpringDataHouseholdRepository householdRepository;

    public JpaInvitationCodeRepository(SpringDataInvitationCodeRepository codeRepository,
                                       SpringDataHouseholdRepository householdRepository) {
        this.codeRepository = codeRepository;
        this.householdRepository = householdRepository;
    }


    @Override
    @Transactional
    public InvitationCode save(InvitationCode invitationCode) {
        // Hibernate flushes inserts before deletes, so the old code must be gone before the new
        // one is inserted, or uq_invitation_codes_household would reject it.
        codeRepository.findByHouseholdId(invitationCode.householdId()).ifPresent(old -> {
            codeRepository.delete(old);
            codeRepository.flush();
        });
        InvitationCodeEntity entity = new InvitationCodeEntity(
                invitationCode.code(),
                householdRepository.getReferenceById(invitationCode.householdId()),
                invitationCode.createdByUserId(),
                invitationCode.createdAt(),
                invitationCode.expiresAt());
        return HouseholdPersistenceMapper.toDomain(codeRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvitationCode> findByCode(String code) {
        return codeRepository.findById(code).map(HouseholdPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvitationCode> findByHouseholdId(UUID householdId) {
        return codeRepository.findByHouseholdId(householdId).map(HouseholdPersistenceMapper::toDomain);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return codeRepository.existsById(code);
    }

    @Override
    @Transactional
    public void deleteByHouseholdId(UUID householdId) {
        codeRepository.deleteByHouseholdId(householdId);
    }
}
