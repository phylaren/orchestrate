package genius.project.orchestrate.user.internal.persistence;

import genius.project.orchestrate.user.internal.UserRepository;
import genius.project.orchestrate.user.internal.domain.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository repository;

    public JpaUserRepository(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = repository.findById(user.id())
                .map(existing -> {
                    existing.setDisplayName(user.displayName());
                    existing.setEmail(user.email());
                    return existing;
                })
                .orElseGet(() -> new UserEntity(user.id(), user.displayName(), user.email(), user.createdAt()));
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID userId) {
        return repository.findById(userId).map(JpaUserRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(JpaUserRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return repository.findAllByOrderByCreatedAtAsc().stream()
                .map(JpaUserRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID userId) {
        return repository.existsById(userId);
    }

    static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getDisplayName(), entity.getEmail(), entity.getCreatedAt());
    }
}
