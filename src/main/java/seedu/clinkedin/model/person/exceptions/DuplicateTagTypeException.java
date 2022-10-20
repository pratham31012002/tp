package seedu.clinkedin.model.person.exceptions;

/**
 * Signals that the operation will result in duplicate tag types.
 */
public class DuplicateTagTypeException extends RuntimeException {
    public DuplicateTagTypeException() {
        super("Operation would result in duplicate tag types");
    }
}
