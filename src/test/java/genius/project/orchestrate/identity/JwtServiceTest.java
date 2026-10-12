package genius.project.orchestrate.identity;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-minimum-32-bytes-long-for-hs256";
    private static final long EXPIRATION_MINUTES = 15;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MINUTES);
    private final JwtValidatorService jwtValidatorService = new JwtValidatorService(SECRET);

    @Test
    void generateToken_producesThreePartJwt() {
        String token = jwtService.generateToken(userDetails("alice@example.com"));

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_encodesSubjectAsUsername() {
        String token = jwtService.generateToken(userDetails("alice@example.com"));

        assertThat(jwtValidatorService.extractUsername(token)).isEqualTo("alice@example.com");
    }

    @Test
    void generateToken_encodesAuthoritiesInRolesClaim() {
        UserDetails user = User.withUsername("alice@example.com")
                .password("irrelevant")
                .authorities("ROLE_USER", "ROLE_ADMIN")
                .build();

        String token = jwtService.generateToken(user);

        Claims claims = jwtValidatorService.extractAllClaims(token);
        assertThat(claims.get("roles", List.class))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void generateToken_setsIssuedAtAndExpirationConsistentWithConfiguredTtl() {
        String token = jwtService.generateToken(userDetails("alice@example.com"));

        Claims claims = jwtValidatorService.extractAllClaims(token);
        long ttlSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;

        assertThat(ttlSeconds).isEqualTo(EXPIRATION_MINUTES * 60);
    }

    @Test
    void getExpirationMillis_returnsConfiguredValueInMillis() {
        assertThat(jwtService.getExpirationMillis()).isEqualTo(EXPIRATION_MINUTES * 60 * 1000);
    }

    @Test
    void constructor_rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtService("short-secret", EXPIRATION_MINUTES))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }

    private UserDetails userDetails(String username) {
        return User.withUsername(username)
                .password("irrelevant")
                .authorities("ROLE_USER")
                .build();
    }
}