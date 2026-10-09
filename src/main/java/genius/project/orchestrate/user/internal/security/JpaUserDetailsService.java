package genius.project.orchestrate.user.internal.security;

import genius.project.orchestrate.user.UserPrincipal;
import genius.project.orchestrate.user.internal.UserRepository;
import genius.project.orchestrate.user.internal.domain.UserCredentials;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Користувача не знайдено");
        }
        String email = username.trim().toLowerCase(Locale.ROOT);
        UserCredentials credentials = userRepository.findCredentialsByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Користувача не знайдено"));
        return new UserPrincipal(
                credentials.id(),
                credentials.email(),
                credentials.displayName(),
                credentials.passwordHash(),
                credentials.enabled());
    }
}