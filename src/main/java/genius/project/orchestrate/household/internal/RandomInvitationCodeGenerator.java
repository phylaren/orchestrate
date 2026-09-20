package genius.project.orchestrate.household.internal;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomInvitationCodeGenerator implements InvitationCodeGenerator {

    // No 0/O and 1/I: codes are read aloud and typed by hand.
    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
