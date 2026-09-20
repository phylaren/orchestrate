package genius.project.orchestrate.household.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RandomInvitationCodeGeneratorTest {

    private final RandomInvitationCodeGenerator generator = new RandomInvitationCodeGenerator();

    @Test
    @DisplayName("код має фіксовану довжину і складається лише з символів алфавіту (без 0/O/1/I)")
    void generatesCodeFromAlphabet() {
        String code = generator.generate();

        assertThat(code).hasSize(RandomInvitationCodeGenerator.LENGTH);
        assertThat(code.chars()).allMatch(c -> RandomInvitationCodeGenerator.ALPHABET.indexOf(c) >= 0);
    }

    @Test
    @DisplayName("послідовні коди практично не повторюються")
    void generatesDistinctCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes).hasSize(1000);
    }
}
