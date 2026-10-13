package genius.project.orchestrate.identity;

import genius.project.orchestrate.identity.dto.CurrentUserResponse;
import genius.project.orchestrate.user.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Issues JWT access tokens")
@RestController
public class CurrentUserController {

    @Operation(summary = "Отримати поточного користувача",
            description = "Повертає користувача, якому належить переданий токен доступу.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Поточний користувач"),
            @ApiResponse(responseCode = "401", description = "Токен відсутній або недійсний "
                    + "(AUTHENTICATION_REQUIRED / INVALID_TOKEN)")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/users/me")
    public CurrentUserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return CurrentUserResponse.from(principal);
    }
}
