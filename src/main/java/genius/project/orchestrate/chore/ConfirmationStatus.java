package genius.project.orchestrate.chore;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Статус підтвердження виконання: NOT_REQUIRED — підтвердження не потрібне, виконання завершене одразу; \
        PENDING — очікує рішення підтверджувача; CONFIRMED — підтверджено; REJECTED — відхилено.""")
public enum ConfirmationStatus {
    NOT_REQUIRED,
    PENDING,
    CONFIRMED,
    REJECTED;

    public boolean canTransitionTo(ConfirmationStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == REJECTED;
            case NOT_REQUIRED, CONFIRMED, REJECTED -> false;
        };
    }
}
