package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.common.exception.ResourceNotFoundException;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static genius.project.orchestrate.household.MembershipPermission.CONFIRM_COMPLETIONS;
import static genius.project.orchestrate.household.MembershipPermission.INVITE_MEMBERS;
import static genius.project.orchestrate.household.MembershipPermission.MANAGE_CHORES;
import static genius.project.orchestrate.household.MembershipPermission.MANAGE_MEMBERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceImplTest {

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private InvitationCodeRepository invitationCodeRepository;

    @Mock
    private InvitationCodeGenerator codeGenerator;

    @Mock
    private UserClient userClient;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private HouseholdServiceImpl service;

    private static final UUID HOUSEHOLD_ID = UUID.randomUUID();
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();
    private static final UUID OTHER_MEMBER = UUID.randomUUID();
    private static final UUID OUTSIDER = UUID.randomUUID();

    @Nested
    @DisplayName("createHousehold")
    class CreateHousehold {

        @Test
        @DisplayName("поточний користувач стає єдиним власником і першим учасником з усіма правами")
        void creatorBecomesOwnerWithAllPermissions() {
            when(currentUserProvider.getUserId()).thenReturn(OWNER);
            when(userClient.existsById(OWNER)).thenReturn(true);
            when(householdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Household created = service.createHousehold("  Квартира на Подолі ");

            assertThat(created.ownerId()).isEqualTo(OWNER);
            assertThat(created.name()).isEqualTo("Квартира на Подолі");
            ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
            verify(membershipRepository).save(captor.capture());
            assertThat(captor.getValue().householdId()).isEqualTo(created.id());
            assertThat(captor.getValue().userId()).isEqualTo(OWNER);
            assertThat(captor.getValue().permissions()).isEqualTo(MembershipPermission.all());
        }

        @Test
        @DisplayName("незареєстрований користувач — USER_NOT_FOUND, дім не створюється")
        void unknownUser_Throws() {
            when(currentUserProvider.getUserId()).thenReturn(OWNER);
            when(userClient.existsById(OWNER)).thenReturn(false);

            assertThatThrownBy(() -> service.createHousehold("Дім"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("USER_NOT_FOUND");
            verifyNoInteractions(householdRepository, membershipRepository);
        }
    }

    @Nested
    @DisplayName("getHousehold")
    class GetHousehold {

        @Test
        @DisplayName("учасник отримує дім")
        void member_GetsHousehold() {
            asCurrentUser(MEMBER);
            Household household = givenHousehold();
            givenMember(MEMBER, CONFIRM_COMPLETIONS);

            assertThat(service.getHousehold(HOUSEHOLD_ID)).isEqualTo(household);
        }

        @Test
        @DisplayName("не учасник — NotHouseholdMemberException")
        void outsider_Forbidden() {
            asCurrentUser(OUTSIDER);
            givenHousehold();
            when(membershipRepository.find(HOUSEHOLD_ID, OUTSIDER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getHousehold(HOUSEHOLD_ID))
                    .isInstanceOf(NotHouseholdMemberException.class);
        }

        @Test
        @DisplayName("неіснуючий дім — HouseholdNotFoundException")
        void unknownHousehold_NotFound() {
            when(householdRepository.findById(HOUSEHOLD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getHousehold(HOUSEHOLD_ID))
                    .isInstanceOf(HouseholdNotFoundException.class)
                    .extracting("errorCode").isEqualTo("HOUSEHOLD_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("deleteHousehold")
    class DeleteHousehold {

        @Test
        @DisplayName("власник видаляє дім разом з учасниками та кодом запрошення")
        void owner_DeletesEverything() {
            asCurrentUser(OWNER);
            givenHousehold();

            service.deleteHousehold(HOUSEHOLD_ID);

            verify(invitationCodeRepository).deleteByHouseholdId(HOUSEHOLD_ID);
            verify(membershipRepository).deleteByHouseholdId(HOUSEHOLD_ID);
            verify(householdRepository).deleteById(HOUSEHOLD_ID);
        }

        @Test
        @DisplayName("не власник — NotHouseholdOwnerException, нічого не видаляється")
        void nonOwner_Forbidden() {
            asCurrentUser(MEMBER);
            givenHousehold();

            assertThatThrownBy(() -> service.deleteHousehold(HOUSEHOLD_ID))
                    .isInstanceOf(NotHouseholdOwnerException.class);
            verify(householdRepository, never()).deleteById(any());
            verify(membershipRepository, never()).deleteByHouseholdId(any());
        }
    }

    @Nested
    @DisplayName("transferOwnership")
    class TransferOwnership {

        @Test
        @DisplayName("власник передає власність учаснику: новий ownerId, новий власник отримує всі права")
        void owner_TransfersToMember() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(MEMBER, CONFIRM_COMPLETIONS);
            when(householdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Household updated = service.transferOwnership(HOUSEHOLD_ID, MEMBER);

            assertThat(updated.ownerId()).isEqualTo(MEMBER);
            ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
            verify(membershipRepository).save(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(MEMBER);
            assertThat(captor.getValue().permissions()).isEqualTo(MembershipPermission.all());
        }

        @Test
        @DisplayName("не власник — NotHouseholdOwnerException, нічого не змінюється")
        void nonOwner_Forbidden() {
            asCurrentUser(MEMBER);
            givenHousehold();

            assertThatThrownBy(() -> service.transferOwnership(HOUSEHOLD_ID, OTHER_MEMBER))
                    .isInstanceOf(NotHouseholdOwnerException.class)
                    .extracting("errorCode").isEqualTo("NOT_HOUSEHOLD_OWNER");
            verify(householdRepository, never()).save(any());
        }

        @Test
        @DisplayName("передача самому собі — OwnershipTransferToSelfException")
        void toSelf_Rejected() {
            asCurrentUser(OWNER);
            givenHousehold();

            assertThatThrownBy(() -> service.transferOwnership(HOUSEHOLD_ID, OWNER))
                    .isInstanceOf(OwnershipTransferToSelfException.class);
            verify(householdRepository, never()).save(any());
        }

        @Test
        @DisplayName("отримувач не учасник дому — MembershipNotFoundException")
        void toNonMember_NotFound() {
            asCurrentUser(OWNER);
            givenHousehold();
            when(membershipRepository.find(HOUSEHOLD_ID, OUTSIDER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transferOwnership(HOUSEHOLD_ID, OUTSIDER))
                    .isInstanceOf(MembershipNotFoundException.class);
            verify(householdRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeMember: вихід із дому")
    class Leave {

        @Test
        @DisplayName("звичайний учасник виходить — його членство видаляється")
        void member_Leaves() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, CONFIRM_COMPLETIONS);

            service.removeMember(HOUSEHOLD_ID, MEMBER);

            verify(membershipRepository).delete(HOUSEHOLD_ID, MEMBER);
            verify(householdRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("власник з іншими учасниками — OwnerMustTransferOwnershipException, нічого не видаляється")
        void ownerWithOthers_MustTransferFirst() {
            asCurrentUser(OWNER);
            givenHousehold();
            Membership owner = givenMember(OWNER, MembershipPermission.all());
            when(membershipRepository.findByHouseholdId(HOUSEHOLD_ID))
                    .thenReturn(List.of(owner, membership(MEMBER, Set.of())));

            assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, OWNER))
                    .isInstanceOf(OwnerMustTransferOwnershipException.class)
                    .extracting("errorCode").isEqualTo("OWNER_MUST_TRANSFER_OWNERSHIP");
            verify(membershipRepository, never()).delete(any(), any());
            verify(householdRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("власник — останній учасник: дім без власника видаляється повністю")
        void lastOwner_DeletesHousehold() {
            asCurrentUser(OWNER);
            givenHousehold();
            Membership owner = givenMember(OWNER, MembershipPermission.all());
            when(membershipRepository.findByHouseholdId(HOUSEHOLD_ID)).thenReturn(List.of(owner));

            service.removeMember(HOUSEHOLD_ID, OWNER);

            verify(invitationCodeRepository).deleteByHouseholdId(HOUSEHOLD_ID);
            verify(membershipRepository).deleteByHouseholdId(HOUSEHOLD_ID);
            verify(householdRepository).deleteById(HOUSEHOLD_ID);
        }
    }

    @Nested
    @DisplayName("removeMember: видалення іншого учасника")
    class RemoveOther {

        @Test
        @DisplayName("учасник з MANAGE_MEMBERS видаляє іншого учасника")
        void manager_RemovesMember() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, MANAGE_MEMBERS);
            givenMember(OTHER_MEMBER, CONFIRM_COMPLETIONS);

            service.removeMember(HOUSEHOLD_ID, OTHER_MEMBER);

            verify(membershipRepository).delete(HOUSEHOLD_ID, OTHER_MEMBER);
        }

        @Test
        @DisplayName("власнику не потрібні збережені права — він завжди може видалити учасника")
        void owner_RemovesMemberRegardlessOfStoredPermissions() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, Set.of());
            givenMember(MEMBER, CONFIRM_COMPLETIONS);

            service.removeMember(HOUSEHOLD_ID, MEMBER);

            verify(membershipRepository).delete(HOUSEHOLD_ID, MEMBER);
        }

        @Test
        @DisplayName("без MANAGE_MEMBERS — MissingPermissionException")
        void withoutPermission_Forbidden() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, CONFIRM_COMPLETIONS);

            assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, OTHER_MEMBER))
                    .isInstanceOf(MissingPermissionException.class)
                    .extracting("errorCode").isEqualTo("MISSING_PERMISSION");
            verify(membershipRepository, never()).delete(any(), any());
        }

        @Test
        @DisplayName("видалити власника не можна — CannotRemoveOwnerException")
        void removingOwner_Rejected() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, MANAGE_MEMBERS);

            assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, OWNER))
                    .isInstanceOf(CannotRemoveOwnerException.class);
            verify(membershipRepository, never()).delete(any(), any());
        }

        @Test
        @DisplayName("ціль не є учасником — MembershipNotFoundException")
        void targetNotMember_NotFound() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            when(membershipRepository.find(HOUSEHOLD_ID, OUTSIDER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, OUTSIDER))
                    .isInstanceOf(MembershipNotFoundException.class);
        }

        @Test
        @DisplayName("ініціатор не учасник дому — NotHouseholdMemberException")
        void actorNotMember_Forbidden() {
            asCurrentUser(OUTSIDER);
            givenHousehold();
            when(membershipRepository.find(HOUSEHOLD_ID, OUTSIDER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, MEMBER))
                    .isInstanceOf(NotHouseholdMemberException.class);
        }
    }

    @Nested
    @DisplayName("updatePermissions")
    class UpdatePermissions {

        @Test
        @DisplayName("учасник з MANAGE_MEMBERS змінює права іншого учасника")
        void manager_UpdatesPermissions() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, MANAGE_MEMBERS);
            givenMember(OTHER_MEMBER, CONFIRM_COMPLETIONS);
            when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Membership updated = service.updatePermissions(HOUSEHOLD_ID, OTHER_MEMBER, Set.of(MANAGE_CHORES, INVITE_MEMBERS));

            assertThat(updated.userId()).isEqualTo(OTHER_MEMBER);
            assertThat(updated.permissions()).containsExactlyInAnyOrder(MANAGE_CHORES, INVITE_MEMBERS);
        }

        @Test
        @DisplayName("можна забрати всі права (порожній набір)")
        void emptySet_Allowed() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            givenMember(MEMBER, CONFIRM_COMPLETIONS);
            when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.updatePermissions(HOUSEHOLD_ID, MEMBER, Set.of()).permissions()).isEmpty();
        }

        @Test
        @DisplayName("права власника незмінні — OwnerPermissionsImmutableException")
        void ownerPermissions_Immutable() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());

            assertThatThrownBy(() -> service.updatePermissions(HOUSEHOLD_ID, OWNER, Set.of()))
                    .isInstanceOf(OwnerPermissionsImmutableException.class);
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("змінювати власні права не можна — SelfPermissionChangeException")
        void selfChange_Rejected() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, MANAGE_MEMBERS);

            assertThatThrownBy(() -> service.updatePermissions(HOUSEHOLD_ID, MEMBER, MembershipPermission.all()))
                    .isInstanceOf(SelfPermissionChangeException.class);
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("без MANAGE_MEMBERS — MissingPermissionException")
        void withoutPermission_Forbidden() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, INVITE_MEMBERS);

            assertThatThrownBy(() -> service.updatePermissions(HOUSEHOLD_ID, OTHER_MEMBER, Set.of()))
                    .isInstanceOf(MissingPermissionException.class);
            verify(membershipRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("InvitationCode: створення, перегляд, відкликання")
    class InvitationCodes {

        @Test
        @DisplayName("учасник з INVITE_MEMBERS створює код, дійсний 7 днів")
        void inviter_CreatesCode() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, INVITE_MEMBERS);
            when(codeGenerator.generate()).thenReturn("ABCD2345");
            when(invitationCodeRepository.existsByCode("ABCD2345")).thenReturn(false);
            when(invitationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InvitationCode code = service.createInvitationCode(HOUSEHOLD_ID);

            assertThat(code.code()).isEqualTo("ABCD2345");
            assertThat(code.householdId()).isEqualTo(HOUSEHOLD_ID);
            assertThat(code.createdByUserId()).isEqualTo(MEMBER);
            assertThat(Duration.between(code.createdAt(), code.expiresAt())).isEqualTo(Duration.ofDays(7));
        }

        @Test
        @DisplayName("при колізії код генерується повторно")
        void collision_Regenerates() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            when(codeGenerator.generate()).thenReturn("TAKEN222", "FREE3333");
            when(invitationCodeRepository.existsByCode("TAKEN222")).thenReturn(true);
            when(invitationCodeRepository.existsByCode("FREE3333")).thenReturn(false);
            when(invitationCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.createInvitationCode(HOUSEHOLD_ID).code()).isEqualTo("FREE3333");
        }

        @Test
        @DisplayName("якщо всі спроби дають колізію — IllegalStateException, нічого не зберігається")
        void persistentCollision_Fails() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            when(codeGenerator.generate()).thenReturn("TAKEN222");
            when(invitationCodeRepository.existsByCode(anyString())).thenReturn(true);

            assertThatThrownBy(() -> service.createInvitationCode(HOUSEHOLD_ID))
                    .isInstanceOf(IllegalStateException.class);
            verify(invitationCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("без INVITE_MEMBERS — MissingPermissionException, код не створюється")
        void withoutPermission_Forbidden() {
            asCurrentUser(MEMBER);
            givenHousehold();
            givenMember(MEMBER, CONFIRM_COMPLETIONS);

            assertThatThrownBy(() -> service.createInvitationCode(HOUSEHOLD_ID))
                    .isInstanceOf(MissingPermissionException.class);
            verifyNoInteractions(codeGenerator);
            verify(invitationCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("getInvitationCode повертає активний код")
        void get_ReturnsActiveCode() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            InvitationCode active = code("ACTIVE22", Instant.now().plus(Duration.ofDays(1)));
            when(invitationCodeRepository.findByHouseholdId(HOUSEHOLD_ID)).thenReturn(Optional.of(active));

            assertThat(service.getInvitationCode(HOUSEHOLD_ID)).isEqualTo(active);
        }

        @Test
        @DisplayName("getInvitationCode для простроченого коду — InvitationCodeNotFoundException")
        void get_ExpiredCode_NotFound() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            when(invitationCodeRepository.findByHouseholdId(HOUSEHOLD_ID))
                    .thenReturn(Optional.of(code("OLD22222", Instant.now().minusSeconds(1))));

            assertThatThrownBy(() -> service.getInvitationCode(HOUSEHOLD_ID))
                    .isInstanceOf(InvitationCodeNotFoundException.class);
        }

        @Test
        @DisplayName("revokeInvitationCode видаляє код")
        void revoke_Deletes() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            when(invitationCodeRepository.findByHouseholdId(HOUSEHOLD_ID))
                    .thenReturn(Optional.of(code("ACTIVE22", Instant.now().plus(Duration.ofDays(1)))));

            service.revokeInvitationCode(HOUSEHOLD_ID);

            verify(invitationCodeRepository).deleteByHouseholdId(HOUSEHOLD_ID);
        }

        @Test
        @DisplayName("revokeInvitationCode без коду — InvitationCodeNotFoundException")
        void revoke_NoCode_NotFound() {
            asCurrentUser(OWNER);
            givenHousehold();
            givenMember(OWNER, MembershipPermission.all());
            when(invitationCodeRepository.findByHouseholdId(HOUSEHOLD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokeInvitationCode(HOUSEHOLD_ID))
                    .isInstanceOf(InvitationCodeNotFoundException.class);
            verify(invitationCodeRepository, never()).deleteByHouseholdId(any());
        }
    }

    @Nested
    @DisplayName("joinByInvitationCode")
    class Join {

        @Test
        @DisplayName("дійсний код (регістр і пробіли не важливі) — членство з правами за замовчуванням")
        void validCode_CreatesMembership() {
            asCurrentUser(MEMBER);
            when(userClient.existsById(MEMBER)).thenReturn(true);
            when(invitationCodeRepository.findByCode("ABCD2345"))
                    .thenReturn(Optional.of(code("ABCD2345", Instant.now().plus(Duration.ofDays(1)))));
            givenHousehold();
            when(membershipRepository.find(HOUSEHOLD_ID, MEMBER)).thenReturn(Optional.empty());
            when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Membership membership = service.joinByInvitationCode("  abcd2345 ");

            assertThat(membership.householdId()).isEqualTo(HOUSEHOLD_ID);
            assertThat(membership.userId()).isEqualTo(MEMBER);
            assertThat(membership.permissions()).isEqualTo(MembershipPermission.defaultForNewMember());
        }

        @Test
        @DisplayName("невідомий код — InvitationCodeNotFoundException")
        void unknownCode_NotFound() {
            asCurrentUser(MEMBER);
            when(userClient.existsById(MEMBER)).thenReturn(true);
            when(invitationCodeRepository.findByCode("NOPE2222")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.joinByInvitationCode("NOPE2222"))
                    .isInstanceOf(InvitationCodeNotFoundException.class)
                    .extracting("errorCode").isEqualTo("INVITATION_CODE_NOT_FOUND");
        }

        @Test
        @DisplayName("прострочений код — InvitationCodeExpiredException, членство не створюється")
        void expiredCode_Rejected() {
            asCurrentUser(MEMBER);
            when(userClient.existsById(MEMBER)).thenReturn(true);
            when(invitationCodeRepository.findByCode("OLD22222"))
                    .thenReturn(Optional.of(code("OLD22222", Instant.now().minusSeconds(1))));

            assertThatThrownBy(() -> service.joinByInvitationCode("OLD22222"))
                    .isInstanceOf(InvitationCodeExpiredException.class)
                    .extracting("errorCode").isEqualTo("INVITATION_CODE_EXPIRED");
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("вже учасник — AlreadyHouseholdMemberException")
        void alreadyMember_Rejected() {
            asCurrentUser(MEMBER);
            when(userClient.existsById(MEMBER)).thenReturn(true);
            when(invitationCodeRepository.findByCode("ABCD2345"))
                    .thenReturn(Optional.of(code("ABCD2345", Instant.now().plus(Duration.ofDays(1)))));
            givenHousehold();
            givenMember(MEMBER, CONFIRM_COMPLETIONS);

            assertThatThrownBy(() -> service.joinByInvitationCode("ABCD2345"))
                    .isInstanceOf(AlreadyHouseholdMemberException.class);
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("незареєстрований користувач — USER_NOT_FOUND, код навіть не перевіряється")
        void unknownUser_Rejected() {
            asCurrentUser(OUTSIDER);
            when(userClient.existsById(OUTSIDER)).thenReturn(false);

            assertThatThrownBy(() -> service.joinByInvitationCode("ABCD2345"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("USER_NOT_FOUND");
            verifyNoInteractions(invitationCodeRepository);
        }
    }

    @Nested
    @DisplayName("listHouseholdsOfUser: один User → декілька Household")
    class ListHouseholdsOfUser {

        @Test
        @DisplayName("повертає всі доми користувача у порядку вступу")
        void returnsAllMemberships() {
            UUID secondHouseholdId = UUID.randomUUID();
            Household first = household();
            Household second = new Household(secondHouseholdId, "Дача", OTHER_MEMBER, Instant.now());
            Membership inFirst = new Membership(HOUSEHOLD_ID, MEMBER, Set.of(), Instant.parse("2026-02-01T00:00:00Z"));
            Membership inSecond = new Membership(secondHouseholdId, MEMBER, Set.of(), Instant.parse("2026-01-01T00:00:00Z"));
            when(userClient.existsById(MEMBER)).thenReturn(true);
            when(membershipRepository.findByUserId(MEMBER)).thenReturn(List.of(inFirst, inSecond));
            when(householdRepository.findById(HOUSEHOLD_ID)).thenReturn(Optional.of(first));
            when(householdRepository.findById(secondHouseholdId)).thenReturn(Optional.of(second));

            List<UserHousehold> result = service.listHouseholdsOfUser(MEMBER);

            assertThat(result).containsExactly(new UserHousehold(second, inSecond), new UserHousehold(first, inFirst));
        }

        @Test
        @DisplayName("неіснуючий користувач — USER_NOT_FOUND")
        void unknownUser_NotFound() {
            when(userClient.existsById(OUTSIDER)).thenReturn(false);

            assertThatThrownBy(() -> service.listHouseholdsOfUser(OUTSIDER))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode").isEqualTo("USER_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("HouseholdClient: isMember / hasPermission")
    class Client {

        @Test
        @DisplayName("isMember і hasPermission відображають збережене членство")
        void reflectsMembership() {
            givenMember(MEMBER, CONFIRM_COMPLETIONS);
            when(membershipRepository.find(HOUSEHOLD_ID, OUTSIDER)).thenReturn(Optional.empty());

            assertThat(service.isMember(HOUSEHOLD_ID, MEMBER)).isTrue();
            assertThat(service.isMember(HOUSEHOLD_ID, OUTSIDER)).isFalse();
            assertThat(service.hasPermission(HOUSEHOLD_ID, MEMBER, CONFIRM_COMPLETIONS)).isTrue();
            assertThat(service.hasPermission(HOUSEHOLD_ID, MEMBER, MANAGE_MEMBERS)).isFalse();
            assertThat(service.hasPermission(HOUSEHOLD_ID, OUTSIDER, CONFIRM_COMPLETIONS)).isFalse();
        }
    }

    // -------------------------------------------------------------------------

    private void asCurrentUser(UUID userId) {
        when(currentUserProvider.getUserId()).thenReturn(userId);
    }

    private Household givenHousehold() {
        Household household = household();
        when(householdRepository.findById(HOUSEHOLD_ID)).thenReturn(Optional.of(household));
        return household;
    }

    private Membership givenMember(UUID userId, MembershipPermission first, MembershipPermission... rest) {
        return givenMember(userId, EnumSet.of(first, rest));
    }

    private Membership givenMember(UUID userId, Set<MembershipPermission> permissions) {
        Membership membership = membership(userId, permissions);
        when(membershipRepository.find(HOUSEHOLD_ID, userId)).thenReturn(Optional.of(membership));
        return membership;
    }

    private static Household household() {
        return new Household(HOUSEHOLD_ID, "Квартира", OWNER, Instant.now());
    }

    private static Membership membership(UUID userId, Set<MembershipPermission> permissions) {
        return new Membership(HOUSEHOLD_ID, userId, permissions, Instant.now());
    }

    private static InvitationCode code(String value, Instant expiresAt) {
        return new InvitationCode(value, HOUSEHOLD_ID, OWNER, Instant.now().minus(Duration.ofDays(1)), expiresAt);
    }
}
