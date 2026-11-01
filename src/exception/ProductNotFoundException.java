package exception;

/** Thrown when a product ID/name does not exist in the inventory. */
public class ProductNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(String key) {
        super("No product found with ID '" + key + "'.");
    }
}
