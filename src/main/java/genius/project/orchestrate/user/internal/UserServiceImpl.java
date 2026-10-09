package genius.project.orchestrate.user.internal;

import genius.project.orchestrate.user.UserService;
import genius.project.orchestrate.user.exception.EmailAlreadyTakenException;
import genius.project.orchestrate.user.exception.UserNotFoundException;
import genius.project.orchestrate.user.internal.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(String displayName, String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyTakenException(normalizedEmail);
        }
        User user = new User(UUID.randomUUID(), displayName.trim(), normalizedEmail, Instant.now());
        User saved = repository.register(user, passwordEncoder.encode(rawPassword));
        log.info("User created: userId={}", saved.id());
        return saved;
    }

    @Override
    public User getUser(UUID userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public List<User> listUsers() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(User::createdAt))
                .toList();
    }

    @Override
    public boolean existsById(UUID userId) {
        return repository.existsById(userId);
    }
}