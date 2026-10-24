package genius.project.orchestrate.user.internal;

import genius.project.orchestrate.user.UserService;
import genius.project.orchestrate.user.UserSummaries;
import genius.project.orchestrate.user.UserSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class UserSummaryDirectory implements UserSummaries {

    private final UserService userService;

    UserSummaryDirectory(UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<UserSummary> listAll() {
        return userService.listUsers().stream()
                .map(user -> new UserSummary(user.id(), user.displayName(), user.email()))
                .toList();
    }
}
