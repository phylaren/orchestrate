package genius.project.orchestrate.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Життєвий цикл браузера для наскрізних тестів: окремий браузер і контекст на кожен тест.
 * Під час тесту зйомка трасування ввімкнена. Після тесту {@code trace.zip} і знімок екрана
 * зберігаються в каталозі {@code e2e.artifacts.dir} (за замовчуванням {@code build/e2e-artifacts}):
 * при падінні завжди, а при успіху — якщо задано {@code e2e.artifacts.always=true}.
 * Сторінка передається в тест параметром {@code Page page}.
 */
public class BrowserTestSupport implements BeforeEachCallback, AfterEachCallback, TestWatcher, ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(BrowserTestSupport.class);
    private static final String SESSION_KEY = "browser-session";
    private static final String FAILED_KEY = "failed";
    private static final String ARTIFACTS_PROPERTY = "e2e.artifacts.dir";
    private static final String ALWAYS_PROPERTY = "e2e.artifacts.always";
    private static final String DEFAULT_ARTIFACTS_DIRECTORY = "build/e2e-artifacts";

    record BrowserSession(Playwright playwright, Browser browser, BrowserContext context, Page page) {
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        BrowserContext browserContext = browser.newContext();
        browserContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        Page page = browserContext.newPage();
        store(context).put(SESSION_KEY, new BrowserSession(playwright, browser, browserContext, page));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        store(context).put(FAILED_KEY, Boolean.TRUE);
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        BrowserSession session = store(context).remove(SESSION_KEY, BrowserSession.class);
        if (session == null) {
            return;
        }
        try {
            boolean failed = Boolean.TRUE.equals(store(context).get(FAILED_KEY, Boolean.class));
            if (failed || Boolean.getBoolean(ALWAYS_PROPERTY)) {
                saveArtifacts(context, session, failed);
            } else {
                session.context().tracing().stop();
            }
        } finally {
            session.context().close();
            session.browser().close();
            session.playwright().close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == Page.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        BrowserSession session = store(extensionContext).get(SESSION_KEY, BrowserSession.class);
        if (session == null) {
            throw new ParameterResolutionException("Браузерну сесію не створено для цього тесту");
        }
        return session.page();
    }

    private static void saveArtifacts(ExtensionContext context, BrowserSession session, boolean failed)
            throws IOException {
        Path directory = artifactsDirectory(context);
        Files.createDirectories(directory);
        String screenshotName = failed ? "failure.png" : "final.png";
        session.page().screenshot(new Page.ScreenshotOptions()
                .setPath(directory.resolve(screenshotName))
                .setFullPage(true));
        session.context().tracing().stop(new Tracing.StopOptions().setPath(directory.resolve("trace.zip")));
    }

    private static Path artifactsDirectory(ExtensionContext context) {
        return Path.of(System.getProperty(ARTIFACTS_PROPERTY, DEFAULT_ARTIFACTS_DIRECTORY))
                .resolve(context.getRequiredTestClass().getSimpleName())
                .resolve(context.getRequiredTestMethod().getName());
    }

    private static ExtensionContext.Store store(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
