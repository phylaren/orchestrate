package genius.project.orchestrate.identity.internal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Підключає до {@code @WebMvcTest} справжню конфігурацію безпеки: SecurityFilterChain, CORS,
 * обробники 401/403 та Method Security. Тест має оголосити {@code @MockitoBean}
 * для {@code JwtValidatorService} і {@code UserDetailsService}.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({
        SecurityConfiguration.class,
        CorsConfig.class,
        SecurityProblemWriter.class,
        ProblemDetailAuthenticationEntryPoint.class,
        ProblemDetailAccessDeniedHandler.class
})
public class SecurityWebMvcTestConfiguration {
}
