package genius.project.orchestrate.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Дані для реєстрації нового користувача")
public record UserCreateRequest(

        @Schema(description = "Ім'я, що відображається іншим мешканцям", example = "Анна Петренко",
                maxLength = 100)
        @NotBlank(message = "displayName must not be blank")
        @Size(max = 100, message = "displayName must be at most 100 characters")
        String displayName,

        @Schema(description = "Електронна пошта; унікальна в системі (регістр не враховується)",
                example = "anna.petrenko@example.com", maxLength = 254)
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a well-formed email address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        @Schema(description = "Пароль (8–128 символів). Зберігається лише як Argon2id-хеш і ніколи не повертається в API",
                example = "correct-horse-battery", minLength = 8, maxLength = 128,
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
        String password
) {

        @Override
        public String toString() {
                return "UserCreateRequest[displayName=" + displayName + ", email=" + email + ", password=***]";
        }
}
