package genius.project.orchestrate.user;

import genius.project.orchestrate.user.client.UserClient;
import genius.project.orchestrate.user.internal.domain.User;

import java.util.List;
import java.util.UUID;

public interface UserService extends UserClient {

    User createUser(String displayName, String email);

    User getUser(UUID userId);

    List<User> listUsers();
}
