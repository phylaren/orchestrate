package genius.project.orchestrate.identity.internal;

import genius.project.orchestrate.identity.JwtValidatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Безсесійна (stateless) конфігурація безпеки REST API.
 * <ul>
 *     <li>сесії не створюються, автентифікація — лише через {@code Authorization: Bearer <JWT>};</li>
 *     <li>CSRF вимкнено: браузер не підставляє Bearer-токен автоматично, кук сесії немає;</li>
 *     <li>публічні маршрути: вхід, реєстрація, документація API та H2-консоль; решта потребує токена;</li>
 *     <li>помилки 401 і 403 повертаються у форматі RFC 9457 ProblemDetail.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration {

    static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/error"
    };

    static final String REGISTRATION_ENDPOINT = "/api/v1/users";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtValidatorService jwtValidatorService,
                                            UserDetailsService userDetailsService,
                                            AuthenticationEntryPoint authEntryPoint,
                                            AccessDeniedHandler accessDeniedHandler) throws Exception {
        // Фільтр створюється тут, а не як бін, щоб Spring Boot не зареєстрував його
        // вдруге як звичайний сервлетний фільтр поза ланцюгом безпеки.
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtValidatorService, userDetailsService, authEntryPoint);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                // H2-консоль відмальовується у фреймах того самого походження.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, REGISTRATION_ENDPOINT).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
