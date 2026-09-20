package genius.project.orchestrate.swap;

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