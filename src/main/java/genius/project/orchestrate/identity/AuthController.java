package genius.project.orchestrate.identity;

import genius.project.orchestrate.identity.dto.AuthResponse;
import genius.project.orchestrate.identity.dto.LoginRequest;
import genius.project.orchestrate.user.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Authentication", description = "Issues JWT access tokens")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final List<TokenClaimsContributor> claimsContributors;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          List<TokenClaimsContributor> claimsContributors) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.claimsContributors = claimsContributors;
    }

    @Operation(summary = "Authenticate and obtain an access token")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials (INVALID_CREDENTIALS)")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(), request.password()));

        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Principal must be UserPrincipal");
        }

        Map<String, Object> extraClaims = new HashMap<>();
        UUID userId = principal.getId();
        for (TokenClaimsContributor contributor : claimsContributors) {
            extraClaims.putAll(contributor.claimsFor(userId));
        }

        String token = jwtService.generateToken(principal, extraClaims);
        long expiresInSeconds = jwtService.getExpirationMillis() / 1000;
        return ResponseEntity.ok(AuthResponse.of(token, expiresInSeconds));
    }
}