package genius.project.orchestrate.identity.web;

import genius.project.orchestrate.identity.UserRole;
import genius.project.orchestrate.identity.UserRoles;
import genius.project.orchestrate.user.UserSummaries;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("@roleAccess.hasRole(authentication, 'ADMIN')")
class UserAdminWebController {

    static final String LIST_VIEW = "admin/users";

    private final UserSummaries userSummaries;
    private final UserRoles userRoles;

    UserAdminWebController(UserSummaries userSummaries, UserRoles userRoles) {
        this.userSummaries = userSummaries;
        this.userRoles = userRoles;
    }

    @GetMapping
    String listUsers(Model model) {
        List<AdminUserRow> rows = userSummaries.listAll().stream()
                .map(user -> new AdminUserRow(
                        user.displayName(),
                        user.email(),
                        userRoles.roleOf(user.id()).orElse(UserRole.USER)))
                .toList();
        model.addAttribute("users", rows);
        return LIST_VIEW;
    }
}
