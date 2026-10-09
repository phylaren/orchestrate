package genius.project.orchestrate.user.internal;

import genius.project.orchestrate.user.internal.domain.User;
import genius.project.orchestrate.user.internal.domain.UserCredentials;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> store = new ConcurrentHashMap<>();
    private final Map<UUID, String> passwordHashes = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        store.put(user.id(), user);
        return user;
    }

    @Override
    public User register(User user, String passwordHash) {
        store.put(user.id(), user);
        passwordHashes.put(user.id(), passwordHash);
        return user;
    }

    @Override
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return findByEmail(email).map(user -> new UserCredentials(
                user.id(), user.email(), user.displayName(), passwordHashes.get(user.id()), true));
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return store.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public boolean existsById(UUID userId) {
        return store.containsKey(userId);
    }
}
