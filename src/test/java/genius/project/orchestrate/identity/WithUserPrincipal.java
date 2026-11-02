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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithUserPrincipal.Factory.class)
public @interface WithUserPrincipal {

    String id() default "00000000-0000-0000-0000-000000000001";

    String email() default "user@orchestrate.test";

    String displayName() default "Test User";

    /**
     * Household permissions for the token. Format: "householdId:PERM1,PERM2".
     */
    String[] households() default {};

    class Factory implements WithSecurityContextFactory<WithUserPrincipal> {

        @Override
        public SecurityContext createSecurityContext(WithUserPrincipal annotation) {
            UserPrincipal principal = new UserPrincipal(UUID.fromString(annotation.id()),
                    annotation.email(), annotation.displayName(), "{argon2}hash", true);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            if (annotation.households().length > 0) {
                Map<String, Object> claims = new HashMap<>();
                Map<String, List<String>> householdPermissions = new HashMap<>();
                for (String h : annotation.households()) {
                    String[] parts = h.split(":");
                    if (parts.length == 2) {
                        householdPermissions.put(parts[0], List.of(parts[1].split(",")));
                    }
                }
                claims.put("households", householdPermissions);
                context.setAuthentication(new TokenAuthentication(principal, claims, null));
            } else {
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
            }
            return context;
        }
    }
}