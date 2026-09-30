package genius.project.orchestrate.household.internal.persistence;

import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;
import genius.project.orchestrate.user.internal.domain.User;
import genius.project.orchestrate.user.internal.persistence.JpaUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static genius.project.orchestrate.household.MembershipPermission.CONFIRM_COMPLETIONS;
import static genius.project.orchestrate.household.MembershipPermission.INVITE_MEMBERS;
import static genius.project.orchestrate.household.MembershipPermission.MANAGE_CHORES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class HouseholdPersistenceTest {

    private static final Instant BASE = Instant.parse("2026-03-01T10:00:00Z");

    @Autowired
    private JpaUserRepository users;

    @Autowired
    private JpaHouseholdRepository households;

    @Autowired
    private JpaMembershipRepository memberships;

    @Autowired
    private JpaInvitationCodeRepository invitationCodes;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private UUID owner;
    private UUID householdId;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        owner = persistUser();
        householdId = persistHousehold(owner);
        addMember(householdId, owner, MembershipPermission.all(), 0);
    }

    // -------------------------------------------------------------------------
    // JOIN FETCH / N+1
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("JOIN FETCH / N+1")
    class FetchStrategy {

        @Test
        @DisplayName("учасники дому разом із правами читаються одним SQL-запитом")
        void membersOfHouseholdIsSingleStatement() {
            for (int i = 1; i <= 4; i++) {
                addMember(householdId, persistUser(), Set.of(CONFIRM_COMPLETIONS, MANAGE_CHORES), i);
            }
            flushAndClear();

            List<Membership> members = memberships.findByHouseholdId(householdId);

            assertThat(members).hasSize(5);
            assertThat(members.getFirst().userId()).isEqualTo(owner);
            assertThat(members.getFirst().permissions()).isEqualTo(MembershipPermission.all());
            assertThat(members.subList(1, 5)).allSatisfy(m ->
                    assertThat(m.permissions()).containsExactlyInAnyOrder(CONFIRM_COMPLETIONS, MANAGE_CHORES));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("усі членства користувача в кількох домах читаються одним SQL-запитом")
        void membershipsOfUserIsSingleStatement() {
            UUID member = persistUser();
            addMember(householdId, member, Set.of(CONFIRM_COMPLETIONS), 1);
            for (int i = 0; i < 2; i++) {
                addMember(persistHousehold(owner), member, Set.of(INVITE_MEMBERS, MANAGE_CHORES), 2 + i);
            }
            flushAndClear();

            List<Membership> result = memberships.findByUserId(member);

            assertThat(result).hasSize(3);
            assertThat(result.getFirst().householdId()).isEqualTo(householdId);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("одне членство з правами читається одним SQL-запитом")
        void singleMembershipIsSingleStatement() {
            flushAndClear();

            Membership membership = memberships.find(householdId, owner).orElseThrow();

            assertThat(membership.permissions()).isEqualTo(MembershipPermission.all());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // cascade / orphanRemoval
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("cascade і orphanRemoval")
    class Cascade {

        @Test
        @DisplayName("видалення дому видаляє членства, їхні права та код запрошення")
        void deletingHouseholdRemovesChildren() {
            addMember(householdId, persistUser(), Set.of(CONFIRM_COMPLETIONS), 1);
            saveCode("CASCADE2", householdId);
            flushAndClear();

            households.deleteById(householdId);
            flushAndClear();

            assertThat(households.findById(householdId)).isEmpty();
            assertThat(count("memberships")).isZero();
            assertThat(count("membership_permissions")).isZero();
            assertThat(count("invitation_codes")).isZero();
        }

        @Test
        @DisplayName("видалення учасника (orphanRemoval) прибирає його членство і права, решта лишається")
        void removingMemberDeletesOrphan() {
            UUID member = persistUser();
            addMember(householdId, member, Set.of(CONFIRM_COMPLETIONS, INVITE_MEMBERS), 1);
            flushAndClear();

            memberships.delete(householdId, member);
            flushAndClear();

            assertThat(memberships.find(householdId, member)).isEmpty();
            assertThat(memberships.findByHouseholdId(householdId)).extracting(Membership::userId).containsExactly(owner);
            assertThat(count("membership_permissions")).isEqualTo(MembershipPermission.all().size());
        }

        @Test
        @DisplayName("учасника, доданого в тій самій транзакції, теж видаляє orphanRemoval")
        void removingMemberAddedInSameTransaction() {
            UUID member = persistUser();
            addMember(householdId, member, Set.of(CONFIRM_COMPLETIONS), 1);

            memberships.delete(householdId, member);
            flushAndClear();

            assertThat(memberships.find(householdId, member)).isEmpty();
        }

        @Test
        @DisplayName("deleteByHouseholdId очищає всі членства, але сам дім лишається")
        void clearingMembershipsKeepsHousehold() {
            addMember(householdId, persistUser(), Set.of(CONFIRM_COMPLETIONS), 1);
            flushAndClear();

            memberships.deleteByHouseholdId(householdId);
            flushAndClear();

            assertThat(households.findById(householdId)).isPresent();
            assertThat(memberships.findByHouseholdId(householdId)).isEmpty();
            assertThat(count("membership_permissions")).isZero();
        }

        @Test
        @DisplayName("повторне збереження членства замінює набір прав у membership_permissions")
        void savingMembershipReplacesPermissions() {
            UUID member = persistUser();
            addMember(householdId, member, Set.of(CONFIRM_COMPLETIONS), 1);
            flushAndClear();

            Membership current = memberships.find(householdId, member).orElseThrow();
            memberships.save(current.withPermissions(Set.of(MANAGE_CHORES, INVITE_MEMBERS)));
            flushAndClear();

            assertThat(memberships.find(householdId, member).orElseThrow().permissions())
                    .containsExactlyInAnyOrder(MANAGE_CHORES, INVITE_MEMBERS);
            assertThat(memberships.find(householdId, member).orElseThrow().joinedAt())
                    .isEqualTo(current.joinedAt());
        }

        @Test
        @DisplayName("передача власності оновлює owner_id дому")
        void savingHouseholdUpdatesOwner() {
            UUID newOwner = persistUser();
            Household household = households.findById(householdId).orElseThrow();
            households.save(household.withOwner(newOwner));
            flushAndClear();

            assertThat(households.findById(householdId).orElseThrow().ownerId()).isEqualTo(newOwner);
        }
    }

    // -------------------------------------------------------------------------
    // invitation codes
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("InvitationCode")
    class InvitationCodes {

        @Test
        @DisplayName("зберігає код і знаходить його за кодом і за домом")
        void savesAndFinds() {
            saveCode("FIRST222", householdId);
            flushAndClear();

            assertThat(invitationCodes.findByCode("FIRST222")).map(InvitationCode::householdId).contains(householdId);
            assertThat(invitationCodes.findByHouseholdId(householdId)).map(InvitationCode::code).contains("FIRST222");
            assertThat(invitationCodes.existsByCode("FIRST222")).isTrue();
            assertThat(invitationCodes.existsByCode("MISSING2")).isFalse();
        }

        @Test
        @DisplayName("новий код замінює попередній: у дому лишається один рядок")
        void newCodeReplacesPrevious() {
            saveCode("FIRST222", householdId);
            flushAndClear();

            saveCode("SECOND22", householdId);
            flushAndClear();

            assertThat(invitationCodes.findByCode("FIRST222")).isEmpty();
            assertThat(invitationCodes.findByHouseholdId(householdId)).map(InvitationCode::code).contains("SECOND22");
            assertThat(count("invitation_codes")).isEqualTo(1);
        }

        @Test
        @DisplayName("deleteByHouseholdId відкликає код")
        void deleteRevokes() {
            saveCode("FIRST222", householdId);
            flushAndClear();

            invitationCodes.deleteByHouseholdId(householdId);
            flushAndClear();

            assertThat(invitationCodes.findByHouseholdId(householdId)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // integrity
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("цілісність даних")
    class Integrity {

        @Test
        @DisplayName("членство для незареєстрованого користувача відхиляє FK memberships → users")
        void membershipRequiresExistingUser() {
            addMember(householdId, UUID.randomUUID(), Set.of(), 1);

            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("дім з незареєстрованим власником відхиляє FK households → users")
        void householdRequiresExistingOwner() {
            households.save(new Household(UUID.randomUUID(), "Нічий", UUID.randomUUID(), BASE));

            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    // -------------------------------------------------------------------------

    private UUID persistUser() {
        UUID id = UUID.randomUUID();
        users.save(new User(id, "User " + id, id + "@example.com", BASE));
        return id;
    }

    private UUID persistHousehold(UUID ownerId) {
        UUID id = UUID.randomUUID();
        households.save(new Household(id, "Дім " + id, ownerId, BASE));
        return id;
    }

    private void addMember(UUID household, UUID userId, Set<MembershipPermission> permissions, int order) {
        memberships.save(new Membership(household, userId, permissions, BASE.plus(Duration.ofMinutes(order))));
    }

    private void saveCode(String code, UUID household) {
        invitationCodes.save(new InvitationCode(code, household, owner, BASE, BASE.plus(Duration.ofDays(7))));
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }
}
