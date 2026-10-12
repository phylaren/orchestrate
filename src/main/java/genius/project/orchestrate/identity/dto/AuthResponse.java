package genius.project.orchestrate.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued access token")
public record AuthResponse(

        @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzM4NCJ9...")
        String accessToken,

        @Schema(description = "Token type per RFC 6750", example = "Bearer")
        String tokenType,

        @Schema(description = "Token lifetime in seconds", example = "900")
        long expiresIn
) {

    public static AuthResponse of(String token, long expiresIn) {
        return new AuthResponse(token, "Bearer", expiresIn);
    }
}