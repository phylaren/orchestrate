package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;
import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.exception.AlreadyHouseholdMemberException;
import genius.project.orchestrate.household.exception.CannotRemoveOwnerException;
import genius.project.orchestrate.household.exception.HouseholdNotFoundException;
import genius.project.orchestrate.household.exception.InvitationCodeExpiredException;
import genius.project.orchestrate.household.exception.InvitationCodeNotFoundException;
import genius.project.orchestrate.household.exception.MembershipNotFoundException;
import genius.project.orchestrate.household.exception.MissingPermissionException;
import genius.project.orchestrate.household.exception.NotHouseholdMemberException;
import genius.project.orchestrate.household.exception.NotHouseholdOwnerException;
import genius.project.orchestrate.household.exception.OwnerMustTransferOwnershipException;
import genius.project.orchestrate.household.exception.OwnerPermissionsImmutableException;
import genius.project.orchestrate.household.exception.OwnershipTransferToSelfException;
import genius.project.orchestrate.household.exception.SelfPermissionChangeException;
import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;
import genius.project.orchestrate.household.internal.domain.UserHousehold;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.user.client.UserClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class HouseholdServiceImpl implements HouseholdService {

    static final Duration INVITATION_CODE_TTL = Duration.ofDays(7);
    static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

    private final HouseholdRepository householdRepository;
    private final MembershipRepository membershipRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final InvitationCodeGenerator codeGenerator;
    private final UserClient userClient;
    private final CurrentUserProvider currentUserProvider;

    public HouseholdServiceImpl(HouseholdRepository householdRepository,
                                MembershipRepository membershipRepository,
                                InvitationCodeRepository invitationCodeRepository,
                                InvitationCodeGenerator codeGenerator,
                                UserClient userClient,
                                CurrentUserProvider currentUserProvider) {
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.invitationCodeRepository = invitationCodeRepository;
        this.codeGenerator = codeGenerator;
        this.userClient = userClient;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Household createHousehold(String name) {
        UUID ownerId = currentUserProvider.getUserId();
        requireUserExists(ownerId);

        Instant now = Instant.now();
        Household household = householdRepository.save(
                new Household(UUID.randomUUID(), name.trim(), ownerId, now));
        membershipRepository.save(new Membership(household.id(), ownerId, MembershipPermission.all(), now));
        return household;
    }

    @Override
    public Household getHousehold(UUID householdId) {
        Household household = getHouseholdOrThrow(householdId);
        requireMembership(householdId, currentUserProvider.getUserId());
        return household;
    }

    @Override
    public void deleteHousehold(UUID householdId) {
        Household household = getHouseholdOrThrow(householdId);
        requireOwner(household, currentUserProvider.getUserId());
        removeHouseholdCompletely(householdId);
    }

    @Override
    public Household transferOwnership(UUID householdId, UUID newOwnerUserId) {
        UUID actorId = currentUserProvider.getUserId();
        Household household = getHouseholdOrThrow(householdId);
        requireOwner(household, actorId);

        if (actorId.equals(newOwnerUserId)) {
            throw new OwnershipTransferToSelfException(householdId);
        }
        Membership newOwnerMembership = membershipRepository.find(householdId, newOwnerUserId)
                .orElseThrow(() -> new MembershipNotFoundException(householdId, newOwnerUserId));

        Household updated = householdRepository.save(household.withOwner(newOwnerUserId));
        membershipRepository.save(newOwnerMembership.withPermissions(MembershipPermission.all()));
        return updated;
    }

    @Override
    public List<Membership> listMembers(UUID householdId) {
        getHouseholdOrThrow(householdId);
        requireMembership(householdId, currentUserProvider.getUserId());
        return membershipRepository.findByHouseholdId(householdId).stream()
                .sorted(Comparator.comparing(Membership::joinedAt))
                .toList();
    }

    @Override
    public Membership getMember(UUID householdId, UUID userId) {
        getHouseholdOrThrow(householdId);
        requireMembership(householdId, currentUserProvider.getUserId());
        return membershipRepository.find(householdId, userId)
                .orElseThrow(() -> new MembershipNotFoundException(householdId, userId));
    }

    @Override
    public void removeMember(UUID householdId, UUID userId) {
        UUID actorId = currentUserProvider.getUserId();
        Household household = getHouseholdOrThrow(householdId);
        Membership actor = requireMembership(householdId, actorId);

        if (actorId.equals(userId)) {
            leave(household, actorId);
            return;
        }

        requirePermission(household, actor, MembershipPermission.MANAGE_MEMBERS);
        if (household.isOwnedBy(userId)) {
            throw new CannotRemoveOwnerException(householdId);
        }
        membershipRepository.find(householdId, userId)
                .orElseThrow(() -> new MembershipNotFoundException(householdId, userId));
        membershipRepository.delete(householdId, userId);
    }

    @Override
    public Membership updatePermissions(UUID householdId, UUID userId, Set<MembershipPermission> permissions) {
        UUID actorId = currentUserProvider.getUserId();
        Household household = getHouseholdOrThrow(householdId);
        Membership actor = requireMembership(householdId, actorId);
        requirePermission(household, actor, MembershipPermission.MANAGE_MEMBERS);

        if (household.isOwnedBy(userId)) {
            throw new OwnerPermissionsImmutableException(householdId);
        }
        if (actorId.equals(userId)) {
            throw new SelfPermissionChangeException(userId);
        }
        Membership target = membershipRepository.find(householdId, userId)
                .orElseThrow(() -> new MembershipNotFoundException(householdId, userId));
        return membershipRepository.save(target.withPermissions(permissions));
    }

    @Override
    public InvitationCode createInvitationCode(UUID householdId) {
        UUID actorId = currentUserProvider.getUserId();
        Household household = getHouseholdOrThrow(householdId);
        requirePermission(household, requireMembership(householdId, actorId), MembershipPermission.INVITE_MEMBERS);

        Instant now = Instant.now();
        return invitationCodeRepository.save(new InvitationCode(
                generateUniqueCode(), householdId, actorId, now, now.plus(INVITATION_CODE_TTL)));
    }

    @Override
    public InvitationCode getInvitationCode(UUID householdId) {
        Household household = getHouseholdOrThrow(householdId);
        requirePermission(household, requireMembership(householdId, currentUserProvider.getUserId()),
                MembershipPermission.INVITE_MEMBERS);

        return invitationCodeRepository.findByHouseholdId(householdId)
                .filter(c -> !c.isExpiredAt(Instant.now()))
                .orElseThrow(() -> new InvitationCodeNotFoundException(householdId));
    }

    @Override
    public void revokeInvitationCode(UUID householdId) {
        Household household = getHouseholdOrThrow(householdId);
        requirePermission(household, requireMembership(householdId, currentUserProvider.getUserId()),
                MembershipPermission.INVITE_MEMBERS);

        if (invitationCodeRepository.findByHouseholdId(householdId).isEmpty()) {
            throw new InvitationCodeNotFoundException(householdId);
        }
        invitationCodeRepository.deleteByHouseholdId(householdId);
    }

    @Override
    public Membership joinByInvitationCode(String code) {
        UUID userId = currentUserProvider.getUserId();
        requireUserExists(userId);

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        InvitationCode invitation = invitationCodeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new InvitationCodeNotFoundException(normalizedCode));
        if (invitation.isExpiredAt(Instant.now())) {
            throw new InvitationCodeExpiredException(normalizedCode);
        }

        UUID householdId = getHouseholdOrThrow(invitation.householdId()).id();
        if (membershipRepository.find(householdId, userId).isPresent()) {
            throw new AlreadyHouseholdMemberException(householdId, userId);
        }
        return membershipRepository.save(new Membership(
                householdId, userId, MembershipPermission.defaultForNewMember(), Instant.now()));
    }

    @Override
    public List<UserHousehold> listHouseholdsOfUser(UUID userId) {
        requireUserExists(userId);
        return membershipRepository.findByUserId(userId).stream()
                .flatMap(m -> householdRepository.findById(m.householdId())
                        .map(h -> new UserHousehold(h, m))
                        .stream())
                .sorted(Comparator.comparing(uh -> uh.membership().joinedAt()))
                .toList();
    }

    @Override
    public boolean isMember(UUID householdId, UUID userId) {
        return membershipRepository.find(householdId, userId).isPresent();
    }

    @Override
    public boolean hasPermission(UUID householdId, UUID userId, MembershipPermission permission) {
        return membershipRepository.find(householdId, userId)
                .map(m -> m.has(permission))
                .orElse(false);
    }

    private void leave(Household household, UUID userId) {
        if (!household.isOwnedBy(userId)) {
            membershipRepository.delete(household.id(), userId);
            return;
        }
        // The owner is always a member, so a single membership means the owner is alone.
        if (membershipRepository.findByHouseholdId(household.id()).size() > 1) {
            throw new OwnerMustTransferOwnershipException(household.id());
        }
        removeHouseholdCompletely(household.id());
    }

    private void removeHouseholdCompletely(UUID householdId) {
        invitationCodeRepository.deleteByHouseholdId(householdId);
        membershipRepository.deleteByHouseholdId(householdId);
        householdRepository.deleteById(householdId);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!invitationCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique invitation code");
    }

    private Household getHouseholdOrThrow(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
    }

    private Membership requireMembership(UUID householdId, UUID userId) {
        return membershipRepository.find(householdId, userId)
                .orElseThrow(() -> new NotHouseholdMemberException(householdId, userId));
    }

    private void requireOwner(Household household, UUID userId) {
        if (!household.isOwnedBy(userId)) {
            throw new NotHouseholdOwnerException(household.id(), userId);
        }
    }

    private void requirePermission(Household household, Membership actor, MembershipPermission permission) {
        if (!household.isOwnedBy(actor.userId()) && !actor.has(permission)) {
            throw new MissingPermissionException(household.id(), actor.userId(), permission);
        }
    }

    private void requireUserExists(UUID userId) {
        if (!userClient.existsById(userId)) {
            throw ResourceNotFoundException.of("user", userId);
        }
    }
}
