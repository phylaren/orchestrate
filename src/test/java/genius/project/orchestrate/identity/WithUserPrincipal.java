package genius.project.orchestrate.identity;

import genius.project.orchestrate.user.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

/**
 * Аналог {@code @WithMockUser}, що кладе в SecurityContext справжній {@link UserPrincipal}
 * (його очікують {@code @AuthenticationPrincipal UserPrincipal} та перевірки {@code @householdAccess}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithUserPrincipal.Factory.class)
public @interface WithUserPrincipal {

    String id() default "00000000-0000-0000-0000-000000000001";

    String email() default "user@orchestrate.test";

    String displayName() default "Test User";

    class Factory implements WithSecurityContextFactory<WithUserPrincipal> {

        @Override
        public SecurityContext createSecurityContext(WithUserPrincipal annotation) {
            UserPrincipal principal = new UserPrincipal(UUID.fromString(annotation.id()),
                    annotation.email(), annotation.displayName(), "{argon2}hash", true);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities()));
            return context;
        }
    }
}
