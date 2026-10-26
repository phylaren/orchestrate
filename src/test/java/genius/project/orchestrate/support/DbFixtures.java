package genius.project.orchestrate.support;

import genius.project.orchestrate.household.MembershipPermission;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class DbFixtures {

    public static final String PASSWORD = "correct-horse-battery";

    private final JdbcClient jdbc;
    private final String passwordHash;

    public DbFixtures(JdbcClient jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordHash = passwordEncoder.encode(PASSWORD);
    }

    public UUID user(String email) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into users (id, display_name, email, created_at, password_hash) "
                        + "values (?, ?, ?, current_timestamp, ?)")
                .params(id, "User " + email, email, passwordHash).update();
        return id;
    }

    public UUID household(UUID ownerId) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into households (id, name, owner_id, created_at) values (?, ?, ?, current_timestamp)")
                .params(id, "Дім", ownerId).update();
        member(id, ownerId, MembershipPermission.values());
        return id;
    }

    public void member(UUID householdId, UUID userId, MembershipPermission... permissions) {
        jdbc.sql("insert into memberships (household_id, user_id, joined_at) values (?, ?, current_timestamp)")
                .params(householdId, userId).update();
        for (MembershipPermission permission : permissions) {
            jdbc.sql("insert into membership_permissions (household_id, user_id, permission) values (?, ?, ?)")
                    .params(householdId, userId, permission.name()).update();
        }
    }

    public UUID chore(UUID householdId, String name, boolean requiresConfirmation) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into chores (id, household_id, name, description, recurrence_days, "
                        + "requires_confirmation, created_at) values (?, ?, ?, null, 7, ?, current_timestamp)")
                .params(id, householdId, name, requiresConfirmation).update();
        return id;
    }

    public void invitationCode(String code, UUID householdId, UUID createdBy) {
        jdbc.sql("insert into invitation_codes (code, household_id, created_by_user_id, created_at, expires_at) "
                        + "values (?, ?, ?, current_timestamp, current_timestamp + interval '7' day)")
                .params(code, householdId, createdBy).update();
    }

    public long count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    public long count(String table, String where, Object... params) {
        return jdbc.sql("select count(*) from " + table + " where " + where)
                .params(params).query(Long.class).single();
    }

    public String text(String sql, Object... params) {
        return jdbc.sql(sql).params(params).query(String.class).single();
    }
}