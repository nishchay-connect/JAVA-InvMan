package model;

import exception.InvalidProductDataException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class StockTransaction {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final String productId;
    private final String productName;
    private final TransactionType type;
    private final int quantity;
    private final double unitPrice;
    private final LocalDateTime timestamp;

    /** Creates a new transaction stamped with the current time. */
    public StockTransaction(String id, String productId, String productName,
                            TransactionType type, int quantity, double unitPrice) {
        this(id, productId, productName, type, quantity, unitPrice, LocalDateTime.now().withNano(0));
    }

    public StockTransaction(String id, String productId, String productName,
                            TransactionType type, int quantity, double unitPrice,
                            LocalDateTime timestamp) {
        if (quantity < 1) {
            throw new InvalidProductDataException("Transaction quantity must be at least 1.");
        }
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public int getNumber() {
        return Integer.parseInt(id.substring(1));
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public TransactionType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotal() {
        return Math.round(quantity * unitPrice * 100.0) / 100.0;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTimestampText() {
        return timestamp.format(FORMAT);
    }

    public String toCsvRow() {
        return String.join(",",
                id,
                productId,
                productName,
                type.name(),
                String.valueOf(quantity),
                String.format(Locale.US, "%.2f", unitPrice),
                String.format(Locale.US, "%.2f", getTotal()),
                getTimestampText());
    }
}
