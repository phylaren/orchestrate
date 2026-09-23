package genius.project.orchestrate.chore.internal.service.strategy;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.exception.SameUserSwapException;
import genius.project.orchestrate.chore.exception.UserNotInRotationException;
import genius.project.orchestrate.chore.internal.domain.RotationSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermanentSwapStrategyTest {

    private static final UUID CHORE_ID = UUID.randomUUID();
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID USER_C = UUID.randomUUID();

    private final PermanentSwapStrategy strategy = new PermanentSwapStrategy();

    @Test
    @DisplayName("type is PERMANENT")
    void type() {
        assertThat(strategy.getSwapType()).isEqualTo(SwapType.PERMANENT);
    }

    @Test
    @DisplayName("swaps two users, responsible not involved: index unchanged")
    void swapUsers_ResponsibleUnchanged() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);

        SwapOutcome outcome = strategy.execute(current, USER_B, USER_C, null);

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result.baseOrder()).containsExactly(USER_A, USER_C, USER_B);
        assertThat(result.currentIndex()).isEqualTo(0);
        assertThat(result.currentResponsible()).isEqualTo(USER_A);
    }

    @Test
    @DisplayName("responsible swapped: currentIndex follows responsible")
    void responsibleSwapped_IndexFollows() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B, USER_C), 0, 1);

        SwapOutcome outcome = strategy.execute(current, USER_A, USER_C, null);

        RotationSchedule result = ((SwapOutcome.ApplyNow) outcome).schedule();
        assertThat(result.baseOrder()).containsExactly(USER_C, USER_B, USER_A);
        assertThat(result.currentIndex()).isEqualTo(2);
        assertThat(result.currentResponsible()).isEqualTo(USER_A);
    }

    @Test
    @DisplayName("user not in group: throws")
    void userNotInGroup_Throws() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 1);
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> strategy.execute(current, USER_A, stranger, null))
                .isInstanceOf(UserNotInRotationException.class)
                .extracting("errorCode").isEqualTo("USER_NOT_IN_ROTATION");
    }

    @Test
    @DisplayName("same user: throws")
    void sameUser_Throws() {
        RotationSchedule current = schedule(List.of(USER_A, USER_B), 0, 1);

        assertThatThrownBy(() -> strategy.execute(current, USER_A, USER_A, null))
                .isInstanceOf(SameUserSwapException.class)
                .extracting("errorCode").isEqualTo("SAME_USER_SWAP");
    }

    private RotationSchedule schedule(List<UUID> order, int index, int cycle) {
        return new RotationSchedule(CHORE_ID, order, index, cycle, Instant.now());
    }
}