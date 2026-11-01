package model;

import exception.InvalidProductDataException;

public class ElectronicsProduct extends Product {

    private static final int MAX_WARRANTY = 120;

    private int warrantyMonths;

    public ElectronicsProduct(String id, String name, String category,
                              double price, int quantity, int minStock, int warrantyMonths) {
        super(id, name, category, price, quantity, minStock);
        if (warrantyMonths < 0 || warrantyMonths > MAX_WARRANTY) {
            throw new InvalidProductDataException(
                    "Warranty must be between 0 and " + MAX_WARRANTY + " months.");
        }
        this.warrantyMonths = warrantyMonths;
    }

    public static int parseWarranty(String raw) {
        return parseInt(raw, "Warranty (months)", 0, MAX_WARRANTY);
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    @Override
    public String getTypeName() {
        return "Electronics";
    }

    @Override
    public String getDetails() {
        return warrantyMonths + " month warranty";
    }

    @Override
    public String getExtra() {
        return String.valueOf(warrantyMonths);
    }

    @Override
    public String getExtraLabel() {
        return "Warranty (months)";
    }

    @Override
    public void validateExtra(String raw) {
        parseWarranty(raw);
    }

    @Override
    public void setExtra(String raw) {
        this.warrantyMonths = parseWarranty(raw);
    }
}
