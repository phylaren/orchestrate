package genius.project.orchestrate.e2e;

import genius.project.orchestrate.support.DbFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Спільні залежності наскрізних тестів: адреса застосунку і фікстури в БД.
 * Перевірки результату робимо через JdbcClient, а не через ті самі сервіси, що виконують дію.
 */
public abstract class BrowserE2ETestBase {

    @LocalServerPort
    private int port;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected DbFixtures fixtures() {
        return new DbFixtures(jdbc, passwordEncoder);
    }
}
