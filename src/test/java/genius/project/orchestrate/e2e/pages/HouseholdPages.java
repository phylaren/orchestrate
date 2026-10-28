package genius.project.orchestrate.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HouseholdPages {

    private static final Pattern BOARD_URL = Pattern.compile("^.*/households/([0-9a-f-]{36})/board$");

    private final Page page;
    private final String baseUrl;

    public HouseholdPages(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /** Створює дім через форму і повертає його ідентифікатор з адреси дошки. */
    public UUID createHousehold(String name) {
        page.navigate(baseUrl + "/households/new");
        page.getByLabel("Назва домогосподарства").fill(name);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Створити")).click();
        page.waitForURL(BOARD_URL);
        Matcher matcher = BOARD_URL.matcher(page.url());
        if (!matcher.matches()) {
            throw new IllegalStateException("Неочікувана адреса після створення: " + page.url());
        }
        return UUID.fromString(matcher.group(1));
    }
}
