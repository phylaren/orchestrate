package genius.project.orchestrate.chore.exception;

import genius.project.orchestrate.common.exception.ValidationException;

public class PositionOutOfBoundsException extends ValidationException {

    public PositionOutOfBoundsException(int position, int size) {
        super("POSITION_OUT_OF_BOUNDS",
                "Position %d is out of bounds [0, %d].".formatted(position, size - 1));
    }
}