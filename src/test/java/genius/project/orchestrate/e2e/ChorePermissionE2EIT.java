package genius.project.orchestrate.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import genius.project.orchestrate.e2e.pages.ChorePages;
import genius.project.orchestrate.e2e.pages.LoginPage;
import genius.project.orchestrate.household.MembershipPermission;
import genius.project.orchestrate.support.DbFixtures;
import genius.project.orchestrate.support.E2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@E2ETest
@ExtendWith(BrowserTestSupport.class)
class ChorePermissionE2EIT extends BrowserE2ETestBase {

    private static final String RESIDENT_EMAIL = "resident.perm.e2e@orchestrate.test";
    private static final String NEW_CHORE_NAME = "Пил E2E";

    @Test
    @DisplayName("Мешканець без права MANAGE_CHORES не бачить кнопки створення")
    void residentWithoutPermission_DoesNotSeeCreateButton(Page page) {
        DbFixtures fixtures = fixtures();
        UUID householdId = fixtures.household(fixtures.user("owner.perm.e2e@orchestrate.test"));
        fixtures.member(householdId, fixtures.user(RESIDENT_EMAIL));
        String baseUrl = baseUrl();

        new LoginPage(page, baseUrl).loginAs(RESIDENT_EMAIL, DbFixtures.PASSWORD);
        new ChorePages(page, baseUrl).openBoard(householdId);

        assertThat(new ChorePages(page, baseUrl).createChoreLink()).hasCount(0);
    }

    @Test
    @DisplayName("Прямий POST на створення обов'язку без права дає 403 і нічого не створює")
    void residentWithoutPermission_DirectPostReturns403(Page page) {
        DbFixtures fixtures = fixtures();
        UUID householdId = fixtures.household(fixtures.user("owner.perm2.e2e@orchestrate.test"));
        fixtures.member(householdId, fixtures.user(RESIDENT_EMAIL));
        String baseUrl = baseUrl();

        new LoginPage(page, baseUrl).loginAs(RESIDENT_EMAIL, DbFixtures.PASSWORD);
        new ChorePages(page, baseUrl).openBoard(householdId);
        String csrfToken = page.locator("input[name='_csrf']").first().getAttribute("value");

        APIResponse response = page.request().post(
                baseUrl + "/households/" + householdId + "/chores",
                RequestOptions.create().setForm(FormData.create()
                        .set("_csrf", csrfToken)
                        .set("name", NEW_CHORE_NAME)
                        .set("recurrenceDays", "7")));

        assertThat(response.status()).isEqualTo(403);
        Long created = jdbc.sql("select count(*) from chores where household_id = :household and name = :name")
                .param("household", householdId)
                .param("name", NEW_CHORE_NAME)
                .query(Long.class)
                .single();
        assertThat(created).isZero();
    }

    @Test
    @DisplayName("Власник з правом MANAGE_CHORES бачить кнопку створення обов'язку")
    void owner_SeesCreateButton(Page page) {
        DbFixtures fixtures = fixtures();
        String ownerEmail = "owner.perm3.e2e@orchestrate.test";
        UUID householdId = fixtures.household(fixtures.user(ownerEmail));
        String baseUrl = baseUrl();

        new LoginPage(page, baseUrl).loginAs(ownerEmail, DbFixtures.PASSWORD);
        new ChorePages(page, baseUrl).openBoard(householdId);

        assertThat(new ChorePages(page, baseUrl).createChoreLink()).isVisible();
    }
}
