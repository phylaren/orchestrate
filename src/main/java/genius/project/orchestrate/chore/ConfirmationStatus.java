package genius.project.orchestrate.chore;

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
