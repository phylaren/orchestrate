package genius.project.orchestrate.user.internal;

import genius.project.orchestrate.user.internal.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID userId);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    boolean existsById(UUID userId);
}
