package genius.project.orchestrate.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;

@Schema(description = "Credentials submitted to obtain an access token")
public record LoginRequest(

        @Schema(description = "User email", example = "anna.petrenko@example.com")
        @NotBlank(message = "username must not be blank")
        String username,

        @Schema(description = "User password", example = "correct-horse-battery",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "password must not be blank")
        String password
) {

    @Override
    public @NonNull String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}