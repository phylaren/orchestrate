package genius.project.orchestrate.identity.dto;

import genius.project.orchestrate.user.UserPrincipal;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.UUID;

@Schema(description = "Автентифікований користувач, визначений за токеном доступу")
public record CurrentUserResponse(
        @Schema(description = "Ідентифікатор користувача", example = "00000000-0000-0000-0000-0000000000d1")
        UUID id,

        @Schema(description = "Електронна пошта (логін)", example = "dev.owner@orchestrate.test")
        String email,

        @Schema(description = "Ім'я для відображення", example = "Dev Owner")
        String displayName,

        @Schema(description = "Надані ролі", example = "[\"ROLE_USER\"]")
        List<String> roles
) {
    public static CurrentUserResponse from(UserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());
    }
}
