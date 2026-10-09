package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.household.HouseholdService;
import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.household.exception.HouseholdNotFoundException;
import genius.project.orchestrate.household.exception.OwnerMustTransferOwnershipException;
import genius.project.orchestrate.household.internal.domain.Household;
import genius.project.orchestrate.household.internal.domain.InvitationCode;
import genius.project.orchestrate.household.internal.domain.Membership;
import genius.project.orchestrate.household.internal.domain.UserHousehold;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Business flow of HouseholdService against the real JPA repositories and H2 schema. */
@SpringBootTest
@Transactional
class HouseholdServiceIntegrationTest {

    @Autowired
    private HouseholdService householdService;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private UUID owner;
    private UUID member;

    @BeforeEach
    void setUp() {
        owner = userService.createUser("Власник", "owner@example.com", "owner-pass-123").id();
        member = userService.createUser("Учасник", "member@example.com", "member-pass-123").id();
    }

    @Test
    @DisplayName("створення → код → приєднання → передача власності → вихід: стан зберігається в БД")
    void fullOwnershipLifecycle() {
        actAs(owner);
        Household household = householdService.createHousehold("Квартира");
        InvitationCode code = householdService.createInvitationCode(household.id());
        flushAndClear();

        actAs(member);
        Membership joined = householdService.joinByInvitationCode(code.code().toLowerCase());
        flushAndClear();
        assertThat(joined.permissions()).isEqualTo(MembershipPermission.defaultForNewMember());

        List<UserHousehold> ofMember = householdService.listHouseholdsOfUser(member);
        assertThat(ofMember).singleElement().satisfies(uh -> {
            assertThat(uh.household().id()).isEqualTo(household.id());
            assertThat(uh.household().isOwnedBy(member)).isFalse();
        });

        actAs(owner);
        assertThatThrownBy(() -> householdService.removeMember(household.id(), owner))
                .isInstanceOf(OwnerMustTransferOwnershipException.class);

        householdService.transferOwnership(household.id(), member);
        flushAndClear();
        assertThat(householdService.getMember(household.id(), member).permissions())
                .isEqualTo(MembershipPermission.all());

        householdService.removeMember(household.id(), owner);
        flushAndClear();

        actAs(member);
        assertThat(householdService.getHousehold(household.id()).ownerId()).isEqualTo(member);
        assertThat(householdService.listMembers(household.id())).extracting(Membership::userId).containsExactly(member);
    }

    @Test
    @DisplayName("останній учасник-власник виходить — дім і його дані видаляються з БД")
    void lastOwnerLeavingDeletesHousehold() {
        actAs(owner);
        Household household = householdService.createHousehold("Дача");
        householdService.createInvitationCode(household.id());
        flushAndClear();

        householdService.removeMember(household.id(), owner);
        flushAndClear();

        assertThatThrownBy(() -> householdService.getHousehold(household.id()))
                .isInstanceOf(HouseholdNotFoundException.class);
        assertThat(householdService.listHouseholdsOfUser(owner)).isEmpty();
    }

    private void actAs(UUID userId) {
        when(currentUserProvider.getUserId()).thenReturn(userId);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
