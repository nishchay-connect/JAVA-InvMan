package exception;

/** Thrown when a sale asks for more units than are in stock. */
public class InsufficientStockException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int available;
    private final int requested;

    public InsufficientStockException(String productName, int available, int requested) {
        super("Not enough stock for '" + productName + "': requested " + requested
                + ", only " + available + " available.");
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
