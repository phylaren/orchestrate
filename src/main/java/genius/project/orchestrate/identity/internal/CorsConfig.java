package genius.project.orchestrate.identity.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Політика CORS для веб-клієнта. Дозволені джерела задаються явним списком (без «*»)
 * у властивості {@code orchestrate.security.cors.allowed-origins}
 * (змінна оточення {@code ORCHESTRATE_SECURITY_CORS_ALLOWED_ORIGINS}).
 * Токен передається в заголовку Authorization, а не в куках, тому allowCredentials = false.
 */
@Configuration(proxyBeanMethods = false)
class CorsConfig {

    static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${orchestrate.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
            List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, TRACE_ID_HEADER));
        config.setExposedHeaders(List.of(TRACE_ID_HEADER, HttpHeaders.LOCATION, HttpHeaders.WWW_AUTHENTICATE));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
