package model;

import exception.InvalidProductDataException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** A perishable item. Extra field: expiry date (yyyy-MM-dd). */
public class GroceryProduct extends Product {

    private LocalDate expiryDate;

    public GroceryProduct(String id, String name, String category,
                          double price, int quantity, int minStock, LocalDate expiryDate) {
        super(id, name, category, price, quantity, minStock);
        if (expiryDate == null) {
            throw new InvalidProductDataException("Expiry date cannot be empty.");
        }
        this.expiryDate = expiryDate;
    }

    public static LocalDate parseDate(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            throw new InvalidProductDataException("Expiry date cannot be empty.");
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new InvalidProductDataException(
                    "Expiry date must be a real date in yyyy-MM-dd format, e.g. 2026-12-31.");
        }
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    @Override
    public String getTypeName() {
        return "Grocery";
    }

    @Override
    public String getDetails() {
        return "Expires " + expiryDate + (isExpired() ? " (EXPIRED)" : "");
    }

    @Override
    public String getExtra() {
        return expiryDate.toString();
    }

    @Override
    public String getExtraLabel() {
        return "Expiry date (yyyy-MM-dd)";
    }

    @Override
    public void validateExtra(String raw) {
        parseDate(raw);
    }

    @Override
    public void setExtra(String raw) {
        this.expiryDate = parseDate(raw);
    }
}
