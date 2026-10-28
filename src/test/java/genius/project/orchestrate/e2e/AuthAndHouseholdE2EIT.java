package genius.project.orchestrate.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import genius.project.orchestrate.e2e.pages.HouseholdPages;
import genius.project.orchestrate.e2e.pages.LoginPage;
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
class AuthAndHouseholdE2EIT extends BrowserE2ETestBase {

    private static final String EMAIL = "owner.e2e@orchestrate.test";
    private static final String HOUSEHOLD_NAME = "Квартира E2E";

    @Test
    @DisplayName("Вхід і створення домогосподарства: відкривається дошка, дім збережено з власником")
    void login_CreateHousehold_OpensBoardAndStoresHousehold(Page page) {
        UUID ownerId = fixtures().user(EMAIL);
        String baseUrl = baseUrl();

        new LoginPage(page, baseUrl).loginAs(EMAIL, DbFixtures.PASSWORD);
        UUID householdId = new HouseholdPages(page, baseUrl).createHousehold(HOUSEHOLD_NAME);

        assertThat(page.getByRole(AriaRole.STATUS))
                .containsText("Домогосподарство «" + HOUSEHOLD_NAME + "» створено.");
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Обов’язки дому")))
                .isVisible();

        Long stored = jdbc.sql("select count(*) from households where id = :id and owner_id = :owner and name = :name")
                .param("id", householdId)
                .param("owner", ownerId)
                .param("name", HOUSEHOLD_NAME)
                .query(Long.class)
                .single();
        assertThat(stored).isEqualTo(1L);
    }
}
