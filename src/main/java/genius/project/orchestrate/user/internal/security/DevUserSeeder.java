package genius.project.orchestrate.user.internal.security;

import genius.project.orchestrate.user.internal.UserRepository;
import genius.project.orchestrate.user.internal.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Profile("dev")
class DevUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevUserSeeder.class);

    record SeedUser(UUID id, String displayName, String email, String intendedRole) {}

    static final List<SeedUser> USERS = List.of(
            new SeedUser(UUID.fromString("00000000-0000-0000-0000-0000000000d1"),
                    "Dev Owner", "dev.owner@orchestrate.test", "власник дому"),
            new SeedUser(UUID.fromString("00000000-0000-0000-0000-0000000000d2"),
                    "Dev Admin", "dev.admin@orchestrate.test", "адміністратор (частина прав)"),
            new SeedUser(UUID.fromString("00000000-0000-0000-0000-0000000000d3"),
                    "Dev Resident", "dev.resident@orchestrate.test", "звичайний мешканець"));

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    DevUserSeeder(UserRepository userRepository,
                  PasswordEncoder passwordEncoder,
                  @Value("${orchestrate.dev-seed.password:dev-password-123}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    void seed() {
        String exampleHash = null;
        for (SeedUser seedUser : USERS) {
            if (userRepository.findByEmail(seedUser.email()).isPresent()) {
                log.debug("Dev user already exists: userId={}", seedUser.id());
                continue;
            }
            String hash = passwordEncoder.encode(password);
            userRepository.register(
                    new User(seedUser.id(), seedUser.displayName(), seedUser.email(), Instant.now()), hash);
            exampleHash = hash;
            log.info("Dev user created: userId={}, intendedRole={}", seedUser.id(), seedUser.intendedRole());
        }
        if (exampleHash != null) {
            log.info("Example Argon2id hash stored in users.password_hash: {}", exampleHash);
        }
    }
}