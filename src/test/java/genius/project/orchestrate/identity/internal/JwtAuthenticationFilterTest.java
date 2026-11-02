package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.JwtValidatorService;
import genius.project.orchestrate.identity.TokenAuthentication;
import genius.project.orchestrate.user.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final UUID HOUSEHOLD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private JwtValidatorService jwtValidatorService;
    private UserDetailsService userDetailsService;
    private AuthenticationEntryPoint authenticationEntryPoint;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtValidatorService = mock(JwtValidatorService.class);
        userDetailsService = mock(UserDetailsService.class);
        authenticationEntryPoint = mock(AuthenticationEntryPoint.class);
        filter = new JwtAuthenticationFilter(
                jwtValidatorService, userDetailsService, authenticationEntryPoint);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    }

    @Test
    void nonBearerAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    }

    @Test
    void validToken_populatesTokenAuthenticationWithClaimsAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        UserPrincipal user = new UserPrincipal(
                UUID.randomUUID(), "alice@example.com", "Alice", "irrelevant", true);

        Claims claims = mock(Claims.class);
        when(claims.get("households")).thenReturn(Map.of(
                HOUSEHOLD_ID.toString(), List.of("MANAGE_CHORES", "INVITE_MEMBERS")));

        when(jwtValidatorService.extractUsername("valid.jwt.token")).thenReturn("alice@example.com");
        when(jwtValidatorService.extractAllClaims("valid.jwt.token")).thenReturn(claims);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        when(jwtValidatorService.isTokenValid("valid.jwt.token", user)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(TokenAuthentication.class);
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        Set<String> permissions = ((TokenAuthentication) authentication).permissionsIn(HOUSEHOLD_ID);
        assertThat(permissions).containsExactlyInAnyOrder("MANAGE_CHORES", "INVITE_MEMBERS");

        verify(chain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    }

    @Test
    void invalidToken_invokesEntryPointAndDoesNotContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtValidatorService.extractUsername("broken.token.value"))
                .thenThrow(new JwtException("bad signature"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void tokenForUnknownUser_invokesEntryPointAndDoesNotContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtValidatorService.extractUsername("valid.jwt.token")).thenReturn("ghost@example.com");
        when(userDetailsService.loadUserByUsername("ghost@example.com"))
                .thenThrow(new UsernameNotFoundException("not found"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void tokenValidButContextAlreadyAuthenticated_doesNotOverwrite() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        UserDetails existing = User.withUsername("admin@example.com")
                .password("irrelevant")
                .authorities("ROLE_ADMIN")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        existing, null, existing.getAuthorities()));

        when(jwtValidatorService.extractUsername("valid.jwt.token")).thenReturn("alice@example.com");

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(existing);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }
}