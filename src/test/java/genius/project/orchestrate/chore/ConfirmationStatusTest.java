package genius.project.orchestrate.chore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static genius.project.orchestrate.chore.ConfirmationStatus.CONFIRMED;
import static genius.project.orchestrate.chore.ConfirmationStatus.NOT_REQUIRED;
import static genius.project.orchestrate.chore.ConfirmationStatus.PENDING;
import static genius.project.orchestrate.chore.ConfirmationStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Перевіряє {@link ConfirmationStatus#canTransitionTo} проти матриці переходів з README.md.
 * Якщо змінюєте автомат — оновіть {@link #ALLOWED} і матрицю в README.
 */
@DisplayName("ConfirmationStatus transitions")
class ConfirmationStatusTest {

    private static final Map<ConfirmationStatus, Set<ConfirmationStatus>> ALLOWED = allowed();

    private static Map<ConfirmationStatus, Set<ConfirmationStatus>> allowed() {
        Map<ConfirmationStatus, Set<ConfirmationStatus>> matrix = new EnumMap<>(ConfirmationStatus.class);
        matrix.put(NOT_REQUIRED, EnumSet.noneOf(ConfirmationStatus.class));
        matrix.put(PENDING, EnumSet.of(CONFIRMED, REJECTED));
        matrix.put(CONFIRMED, EnumSet.noneOf(ConfirmationStatus.class));
        matrix.put(REJECTED, EnumSet.noneOf(ConfirmationStatus.class));
        return matrix;
    }

    static Stream<Arguments> allPairs() {
        Stream.Builder<Arguments> pairs = Stream.builder();
        for (ConfirmationStatus from : ConfirmationStatus.values()) {
            for (ConfirmationStatus to : ConfirmationStatus.values()) {
                pairs.add(Arguments.of(from, to, ALLOWED.get(from).contains(to)));
            }
        }
        return pairs.build();
    }

    @Test
    @DisplayName("matrix covers every enum value")
    void matrixCoversAllValues() {
        assertThat(ALLOWED.keySet()).containsExactlyInAnyOrder(ConfirmationStatus.values());
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("allPairs")
    @DisplayName("canTransitionTo matches the documented matrix")
    void canTransitionTo_matchesMatrix(ConfirmationStatus from, ConfirmationStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(ConfirmationStatus.class)
    @DisplayName("transition to null is never allowed")
    void transitionToNull_isNotAllowed(ConfirmationStatus from) {
        assertThat(from.canTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ConfirmationStatus.class, names = {"NOT_REQUIRED", "CONFIRMED", "REJECTED"})
    @DisplayName("terminal states have no outgoing transitions")
    void terminalStates_haveNoOutgoingTransitions(ConfirmationStatus terminal) {
        for (ConfirmationStatus to : ConfirmationStatus.values()) {
            assertThat(terminal.canTransitionTo(to))
                    .as("%s -> %s", terminal, to)
                    .isFalse();
        }
    }
}