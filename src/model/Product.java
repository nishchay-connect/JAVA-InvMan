package model;

import exception.InsufficientStockException;
import exception.InvalidProductDataException;

import java.util.Locale;

public abstract class Product {

    public static final int MAX_QUANTITY = 1_000_000;
    public static final double MAX_PRICE = 10_000_000;

    public static final String STATUS_OK = "OK";
    public static final String STATUS_LOW = "LOW STOCK";
    public static final String STATUS_OUT = "OUT OF STOCK";

    private final String id;
    private String name;
    private String category;
    private double price;
    private int quantity;
    private int minStock;

    protected Product(String id, String name, String category,
                      double price, int quantity, int minStock) {
        this.id = validateId(id);
        this.name = validateName(name);
        this.category = validateCategory(category);
        this.price = validatePrice(price);
        this.quantity = validateQuantity(quantity);
        this.minStock = validateMinStock(minStock);
    }

  

    public abstract String getTypeName();

    public abstract String getDetails();

    public abstract String getExtra();

    public abstract String getExtraLabel();

    public abstract void validateExtra(String raw);

    public abstract void setExtra(String raw);

    // ------------------------------------------------------------------
    // Stock operations
    // ------------------------------------------------------------------

    public void increaseStock(int amount) {
        if (amount < 1) {
            throw new InvalidProductDataException("Quantity must be at least 1.");
        }
        if ((long) quantity + amount > MAX_QUANTITY) {
            throw new InvalidProductDataException("Stock for '" + name + "' cannot exceed "
                    + MAX_QUANTITY + " units (current: " + quantity + ").");
        }
        quantity += amount;
    }

    public void decreaseStock(int amount) throws InsufficientStockException {
        if (amount < 1) {
            throw new InvalidProductDataException("Quantity must be at least 1.");
        }
        if (amount > quantity) {
            throw new InsufficientStockException(name, quantity, amount);
        }
        quantity -= amount;
    }

    public double getStockValue() {
        return price * quantity;
    }

    public String getStatus() {
        if (quantity == 0) {
            return STATUS_OUT;
        }
        if (quantity <= minStock) {
            return STATUS_LOW;
        }
        return STATUS_OK;
    }

    public boolean isLowStock() {
        return quantity <= minStock;
    }

    // Getters / setters 

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateName(name);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = validateCategory(category);
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = validatePrice(price);
    }

    public int getQuantity() {
        return quantity;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = validateMinStock(minStock);
    }

    public String toCsvRow() {
        return String.join(",",
                getTypeName().toUpperCase(Locale.ROOT),
                id,
                name,
                category,
                String.format(Locale.US, "%.2f", price),
                String.valueOf(quantity),
                String.valueOf(minStock),
                getExtra());
    }


    public static String validateId(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) {
            throw new InvalidProductDataException("Product ID cannot be empty.");
        }
        if (!v.matches("[A-Z0-9_-]{1,15}")) {
            throw new InvalidProductDataException(
                    "Product ID may only contain letters, digits, '-' and '_' (max 15 characters).");
        }
        return v;
    }

    public static String validateName(String raw) {
        return cleanText(raw, "Product name", 50);
    }

    public static String validateCategory(String raw) {
        return cleanText(raw, "Category", 30);
    }

    private static String cleanText(String raw, String field, int maxLength) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw new InvalidProductDataException(field + " cannot be empty.");
        }
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0) {
            throw new InvalidProductDataException(field + " cannot contain commas or quotes.");
        }
        for (char c : v.toCharArray()) {
            if (Character.isISOControl(c)) {
                throw new InvalidProductDataException(field + " contains invalid characters.");
            }
        }
        if (v.length() > maxLength) {
            throw new InvalidProductDataException(field + " is too long (max " + maxLength + " characters).");
        }
        return v;
    }

    public static double validatePrice(double p) {
        if (Double.isNaN(p) || Double.isInfinite(p)) {
            throw new InvalidProductDataException("Price must be a valid number.");
        }
        if (p < 0) {
            throw new InvalidProductDataException("Price cannot be negative.");
        }
        if (p > MAX_PRICE) {
            throw new InvalidProductDataException("Price cannot exceed " + (long) MAX_PRICE + ".");
        }
        double rounded = Math.round(p * 100.0) / 100.0;
        if (rounded <= 0) {
            throw new InvalidProductDataException("Price must be greater than zero.");
        }
        return rounded;
    }

    public static double parsePrice(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            throw new InvalidProductDataException("Price cannot be empty.");
        }
        if (!s.matches("-?\\d{1,12}(\\.\\d+)?")) {
            throw new InvalidProductDataException("Price must be a plain number such as 800 or 49.99.");
        }
        return validatePrice(Double.parseDouble(s));
    }

    public static int validateQuantity(int q) {
        if (q < 0) {
            throw new InvalidProductDataException("Quantity cannot be negative.");
        }
        if (q > MAX_QUANTITY) {
            throw new InvalidProductDataException("Quantity cannot exceed " + MAX_QUANTITY + ".");
        }
        return q;
    }

    public static int validateMinStock(int q) {
        if (q < 0) {
            throw new InvalidProductDataException("Minimum stock cannot be negative.");
        }
        if (q > MAX_QUANTITY) {
            throw new InvalidProductDataException("Minimum stock cannot exceed " + MAX_QUANTITY + ".");
        }
        return q;
    }

    public static int parseQuantity(String raw) {
        return parseInt(raw, "Quantity", 0, MAX_QUANTITY);
    }

    public static int parseMinStock(String raw) {
        return parseInt(raw, "Minimum stock", 0, MAX_QUANTITY);
    }

    public static int parseTransactionQuantity(String raw) {
        return parseInt(raw, "Quantity", 1, MAX_QUANTITY);
    }

    static int parseInt(String raw, String field, int min, int max) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            throw new InvalidProductDataException(field + " cannot be empty.");
        }
        if (!s.matches("-?\\d{1,10}")) {
            throw new InvalidProductDataException(field + " must be a whole number.");
        }
        long v = Long.parseLong(s);
        if (v < 0 && min >= 0) {
            throw new InvalidProductDataException(field + " cannot be negative.");
        }
        if (v < min || v > max) {
            throw new InvalidProductDataException(field + " must be between " + min + " and " + max + ".");
        }
        return (int) v;
    }
}
