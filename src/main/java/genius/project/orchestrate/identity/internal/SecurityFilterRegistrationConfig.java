package genius.project.orchestrate.identity.internal;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Реєструє ланцюг Spring Security у сервлет-контейнері з явним порядком.
 * Без стартера spring-boot-starter-security цього не робить автоконфігурація.
 */
@Configuration(proxyBeanMethods = false)
class SecurityFilterRegistrationConfig {

    /** Той самий порядок, що й у стандартній автоконфігурації Spring Boot Security. */
    static final int SECURITY_FILTER_ORDER = -100;

    @Bean
    FilterRegistrationBean<Filter> securityFilterChainRegistration(
            @Qualifier(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME) Filter springSecurityFilterChain) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(springSecurityFilterChain);
        registration.setName(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
        registration.setOrder(SECURITY_FILTER_ORDER);
        return registration;
    }
}
