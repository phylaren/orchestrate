package genius.project.orchestrate.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class LoginPage {

    private final Page page;
    private final String baseUrl;

    public LoginPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public void loginAs(String email, String password) {
        page.navigate(baseUrl + "/login");
        page.getByLabel("Електронна пошта").fill(email);
        page.getByLabel("Пароль").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти")).click();
        page.waitForURL(baseUrl + "/households");
    }
}
