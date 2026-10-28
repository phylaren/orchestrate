package genius.project.orchestrate.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import genius.project.orchestrate.e2e.pages.ChorePages;
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
class ChoreParticipationE2EIT extends BrowserE2ETestBase {

    private static final String CHORE_NAME = "Винести сміття";
    private static final String RESIDENT_EMAIL = "resident.e2e@orchestrate.test";

    @Test
    @DisplayName("Приєднання до обов'язку і позначка «виконано» записують виконання від імені мешканця")
    void join_ThenMarkCompleted_RecordsCompletion(Page page) {
        DbFixtures fixtures = fixtures();
        UUID ownerId = fixtures.user("owner.chore.e2e@orchestrate.test");
        UUID residentId = fixtures.user(RESIDENT_EMAIL);
        UUID householdId = fixtures.household(ownerId);
        fixtures.member(householdId, residentId);
        UUID choreId = fixtures.chore(householdId, CHORE_NAME, false);
        String baseUrl = baseUrl();
        ChorePages chores = new ChorePages(page, baseUrl);

        new LoginPage(page, baseUrl).loginAs(RESIDENT_EMAIL, DbFixtures.PASSWORD);
        chores.openBoard(householdId);
        chores.join(CHORE_NAME);

        assertThat(chores.row(CHORE_NAME).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Позначити виконаним"))).isVisible();

        chores.markCompleted(CHORE_NAME);

        assertThat(page.getByRole(AriaRole.STATUS)).containsText("Виконання відмічено.");
        Long completions = jdbc.sql("select count(*) from chore_completions "
                        + "where chore_id = :chore and completed_by_user_id = :user")
                .param("chore", choreId)
                .param("user", residentId)
                .query(Long.class)
                .single();
        assertThat(completions).isEqualTo(1L);
    }
}
