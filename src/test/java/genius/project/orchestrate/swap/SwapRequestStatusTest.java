package genius.project.orchestrate.swap;

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

import static genius.project.orchestrate.swap.SwapRequestStatus.ACCEPTED;
import static genius.project.orchestrate.swap.SwapRequestStatus.PENDING;
import static genius.project.orchestrate.swap.SwapRequestStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Перевіряє {@link SwapRequestStatus#canTransitionTo} проти матриці переходів з README.md.
 * Якщо змінюєте автомат — оновіть {@link #ALLOWED} і матрицю в README.
 */
@DisplayName("SwapRequestStatus transitions")
class SwapRequestStatusTest {

    private static final Map<SwapRequestStatus, Set<SwapRequestStatus>> ALLOWED = allowed();

    private static Map<SwapRequestStatus, Set<SwapRequestStatus>> allowed() {
        Map<SwapRequestStatus, Set<SwapRequestStatus>> matrix = new EnumMap<>(SwapRequestStatus.class);
        matrix.put(PENDING, EnumSet.of(ACCEPTED, REJECTED));
        matrix.put(ACCEPTED, EnumSet.noneOf(SwapRequestStatus.class));
        matrix.put(REJECTED, EnumSet.noneOf(SwapRequestStatus.class));
        return matrix;
    }

    static Stream<Arguments> allPairs() {
        Stream.Builder<Arguments> pairs = Stream.builder();
        for (SwapRequestStatus from : SwapRequestStatus.values()) {
            for (SwapRequestStatus to : SwapRequestStatus.values()) {
                pairs.add(Arguments.of(from, to, ALLOWED.get(from).contains(to)));
            }
        }
        return pairs.build();
    }

    @Test
    @DisplayName("matrix covers every enum value")
    void matrixCoversAllValues() {
        assertThat(ALLOWED.keySet()).containsExactlyInAnyOrder(SwapRequestStatus.values());
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("allPairs")
    @DisplayName("canTransitionTo matches the documented matrix")
    void canTransitionTo_matchesMatrix(SwapRequestStatus from, SwapRequestStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(SwapRequestStatus.class)
    @DisplayName("transition to null is never allowed")
    void transitionToNull_isNotAllowed(SwapRequestStatus from) {
        assertThat(from.canTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SwapRequestStatus.class, names = {"ACCEPTED", "REJECTED"})
    @DisplayName("terminal states have no outgoing transitions")
    void terminalStates_haveNoOutgoingTransitions(SwapRequestStatus terminal) {
        for (SwapRequestStatus to : SwapRequestStatus.values()) {
            assertThat(terminal.canTransitionTo(to))
                    .as("%s -> %s", terminal, to)
                    .isFalse();
        }
    }
}