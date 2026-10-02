package genius.project.orchestrate.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveDataMaskingConverter")
class SensitiveDataMaskingConverterTest {

    private final SensitiveDataMaskingConverter converter = new SensitiveDataMaskingConverter();

    @ParameterizedTest
    @CsvSource({
            "user anna@example.com registered, user ***EMAIL*** registered",
            "contact john.doe+tag@sub.domain.co.uk, contact ***EMAIL***",
            "email=anna@example.com, email=***EMAIL***"
    })
    @DisplayName("masks email addresses")
    void masksEmail(String input, String expected) {
        assertThat(convert(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("masks a JWT passed as a plain string argument")
    void masksJwt() {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        assertThat(convert("token=" + token))
                .isEqualTo("token=***JWT***");
    }

    @Test
    @DisplayName("masks a JWT inside a larger message")
    void masksJwtInsideLargerMessage() {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        assertThat(convert("Login request token=" + token + " for user anna@example.com"))
                .isEqualTo("Login request token=***JWT*** for user ***EMAIL***");
    }

    @Test
    @DisplayName("leaves UUIDs untouched")
    void leavesUuidsUntouched() {
        String input = "Household created: householdId=44444444-4444-4444-4444-444444444444, ownerId=00000000-0000-0000-0000-000000000001";
        assertThat(convert(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("leaves fully-qualified class names untouched")
    void leavesFqcnsUntouched() {
        String input = "genius.project.orchestrate.household.internal.persistence.HouseholdEntity";
        assertThat(convert(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("null formatted message becomes empty string")
    void nullMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        assertThat(converter.convert(event)).isEmpty();
    }

    private String convert(String message) {
        LoggingEvent event = new LoggingEvent();
        event.setMessage(message);
        event.setLevel(Level.INFO);
        return converter.convert(event);
    }
}