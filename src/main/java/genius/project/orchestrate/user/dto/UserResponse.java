package genius.project.orchestrate.user.dto;

import genius.project.orchestrate.user.internal.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Користувач системи")
public record UserResponse(
        @Schema(description = "Ідентифікатор користувача", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Ім'я, що відображається", example = "Анна Петренко")
        String displayName,

        @Schema(description = "Електронна пошта (нормалізована до нижнього регістру)",
                example = "anna.petrenko@example.com")
        String email,

        @Schema(description = "Момент створення облікового запису", example = "2026-10-05T10:15:30Z")
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.displayName(), user.email(), user.createdAt());
    }
}
