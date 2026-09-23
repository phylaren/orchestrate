package genius.project.orchestrate.common.exception;

import org.springframework.modulith.NamedInterface;

@NamedInterface
public class InvalidCycleNumberException extends ValidationException {

    private InvalidCycleNumberException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidCycleNumberException missing() {
        return new InvalidCycleNumberException(
                "MISSING_CYCLE_NUMBER",
                "TEMPORARY swap requires a cycleNumber.");
    }

    public static InvalidCycleNumberException inPast(int requested, int current) {
        return new InvalidCycleNumberException(
                "CYCLE_IN_PAST",
                "cycleNumber %d is not a future cycle (current cycle is %d)."
                        .formatted(requested, current));
    }

    public static InvalidCycleNumberException tooFar(int requested, int max, int current) {
        return new InvalidCycleNumberException(
                "CYCLE_TOO_FAR",
                "cycleNumber %d is more than %d cycles ahead (current cycle is %d)."
                        .formatted(requested, max, current));
    }
}