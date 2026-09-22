package genius.project.orchestrate.user.internal;

import genius.project.orchestrate.user.UserService;
import genius.project.orchestrate.user.exception.EmailAlreadyTakenException;
import genius.project.orchestrate.user.exception.UserNotFoundException;
import genius.project.orchestrate.user.internal.domain.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User createUser(String displayName, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyTakenException(normalizedEmail);
        }
        User user = new User(UUID.randomUUID(), displayName.trim(), normalizedEmail, Instant.now());
        return repository.save(user);
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
