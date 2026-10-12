package genius.project.orchestrate.identity;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtValidatorServiceTest {

    private static final String SECRET = "test-secret-key-minimum-32-bytes-long-for-hs256";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JwtService jwtService = new JwtService(SECRET, 15);
    private final JwtValidatorService validator = new JwtValidatorService(SECRET);

    @Test
    void isTokenValid_returnsTrueForFreshTokenOfMatchingUser() {
        UserDetails user = userDetails("alice@example.com");
        String token = jwtService.generateToken(user);

        assertThat(validator.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseWhenSubjectDoesNotMatchUser() {
        String token = jwtService.generateToken(userDetails("alice@example.com"));
        UserDetails other = userDetails("bob@example.com");

        assertThat(validator.isTokenValid(token, other)).isFalse();
    }

    @Test
    void isTokenValid_throwsForExpiredToken() {
        String expired = Jwts.builder()
                .subject("alice@example.com")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(KEY)
                .compact();

        assertThatThrownBy(() -> validator.isTokenValid(expired, userDetails("alice@example.com")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractAllClaims_throwsForTokenSignedWithDifferentKey() {
        SecretKey foreignKey = Keys.hmacShaKeyFor(
                "another-secret-key-minimum-32-bytes-long-for-hs256".getBytes(StandardCharsets.UTF_8));
        String foreignToken = Jwts.builder()
                .subject("alice@example.com")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(foreignKey)
                .compact();

        assertThatThrownBy(() -> validator.extractAllClaims(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractAllClaims_throwsForMalformedToken() {
        assertThatThrownBy(() -> validator.extractAllClaims("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken(userDetails("alice@example.com"));

        assertThat(validator.extractUsername(token)).isEqualTo("alice@example.com");
    }

    @Test
    void constructor_rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtValidatorService("short-secret"))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }

    private UserDetails userDetails(String username) {
        return User.withUsername(username)
                .password("irrelevant")
                .authorities("ROLE_USER")
                .build();
    }
}