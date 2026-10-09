package genius.project.orchestrate.user.internal;

import genius.project.orchestrate.user.internal.domain.User;
import genius.project.orchestrate.user.internal.domain.UserCredentials;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    User register(User user, String passwordHash);
    Optional<User> findById(UUID userId);
    Optional<User> findByEmail(String email);
    Optional<UserCredentials> findCredentialsByEmail(String email);
    List<User> findAll();
    boolean existsById(UUID userId);
}
