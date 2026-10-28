package genius.project.orchestrate.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.UUID;

public class ChorePages {

    private final Page page;
    private final String baseUrl;

    public ChorePages(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public void openBoard(UUID householdId) {
        page.navigate(baseUrl + "/households/" + householdId + "/board");
    }

    public void join(String choreName) {
        row(choreName).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Приєднатися")).click();
    }

    public void markCompleted(String choreName) {
        row(choreName).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Позначити виконаним")).click();
    }

    public Locator row(String choreName) {
        return page.locator("tr").filter(new Locator.FilterOptions().setHasText(choreName));
    }

    public Locator createChoreLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Додати обов’язок"));
    }
}
