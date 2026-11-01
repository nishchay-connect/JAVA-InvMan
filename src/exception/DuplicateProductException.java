package exception;

/** Thrown when someone tries to register a product whose ID is already used. */
public class DuplicateProductException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateProductException(String id) {
        super("A product with ID '" + id + "' already exists.");
    }
}
