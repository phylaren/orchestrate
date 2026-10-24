package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.UserRole;
import genius.project.orchestrate.identity.UserRoles;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class JdbcUserRoles implements UserRoles {

    private final JdbcClient jdbcClient;

    JdbcUserRoles(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<UserRole> roleOf(UUID userId) {
        return jdbcClient.sql("SELECT role FROM users WHERE id = :id")
                .param("id", userId)
                .query(String.class)
                .optional()
                .map(UserRole::valueOf);
    }
}
