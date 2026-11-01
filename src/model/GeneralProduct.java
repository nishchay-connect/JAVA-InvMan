package model;

/** A plain product with no type-specific data (stationery, accessories, ...). */
public class GeneralProduct extends Product {

    public GeneralProduct(String id, String name, String category,
                          double price, int quantity, int minStock) {
        super(id, name, category, price, quantity, minStock);
    }

    @Override
    public String getTypeName() {
        return "General";
    }

    @Override
    public String getDetails() {
        return "-";
    }

    @Override
    public String getExtra() {
        return "";
    }

    @Override
    public String getExtraLabel() {
        return "";
    }

    @Override
    public void validateExtra(String raw) {
    }

    @Override
    public void setExtra(String raw) {
    }
}
