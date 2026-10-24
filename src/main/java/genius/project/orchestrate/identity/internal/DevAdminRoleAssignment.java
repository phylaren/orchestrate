package genius.project.orchestrate.identity.internal;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Dev-профіль створює тестових користувачів після міграцій, тож роль адміністратора
 * для {@code dev.admin} виставляється окремо, коли застосунок уже піднявся.
 */
@Component
@Profile("dev")
class DevAdminRoleAssignment {

    static final String DEV_ADMIN_EMAIL = "dev.admin@orchestrate.test";

    private final JdbcClient jdbcClient;

    DevAdminRoleAssignment(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    void assignAdminRole() {
        jdbcClient.sql("UPDATE users SET role = 'ADMIN' WHERE email = :email")
                .param("email", DEV_ADMIN_EMAIL)
                .update();
    }
}
