package genius.project.orchestrate.common.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Реєструє {@link TraceIdFilter} раніше за ланцюг Spring Security (порядок -100),
 * щоб заголовок {@code X-Trace-Id} і MDC були також у відповідях 401/403, сформованих фільтрами безпеки.
 */
@Configuration(proxyBeanMethods = false)
class TraceIdFilterRegistrationConfig {

    static final int TRACE_ID_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter traceIdFilter) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(traceIdFilter);
        registration.setOrder(TRACE_ID_FILTER_ORDER);
        return registration;
    }
}
