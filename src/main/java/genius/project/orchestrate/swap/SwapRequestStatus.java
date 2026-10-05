package genius.project.orchestrate.swap;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Статус запиту на обмін: PENDING — очікує відповіді отримувача; \
        ACCEPTED — прийнято; REJECTED — відхилено. З PENDING можливий перехід лише в ACCEPTED або REJECTED.""")
public enum SwapRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED;

    public boolean canTransitionTo(SwapRequestStatus next) {
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == REJECTED;
            case ACCEPTED, REJECTED -> false;
        };
    }
}