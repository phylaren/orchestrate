package genius.project.orchestrate.chore.exception;

import genius.project.orchestrate.common.exception.ForbiddenException;

import java.util.UUID;

public class InsertSelfNotAllowedException extends ForbiddenException {

    public InsertSelfNotAllowedException(UUID userId) {
        super("INSERT_SELF_NOT_ALLOWED",
                "Admin cannot insert themselves via INSERT (user '%s').".formatted(userId));
    }
}