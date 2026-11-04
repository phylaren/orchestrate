package genius.project.orchestrate.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@TestConfiguration(proxyBeanMethods = false)
public class RequestInfoTestConfiguration {

    public static final String PATH = "/api/v1/auth/test-request-info";

    @Bean
    RouterFunction<ServerResponse> requestInfoRoute() {
        return RouterFunctions.route()
                .GET(PATH, request -> {
                    HttpServletRequest servlet = request.servletRequest();
                    String body = "remoteAddr=%s;scheme=%s;secure=%s"
                            .formatted(servlet.getRemoteAddr(), servlet.getScheme(), servlet.isSecure());
                    return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body(body);
                })
                .build();
    }
}
