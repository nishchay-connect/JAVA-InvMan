package report;

import model.Product;
import service.Inventory;
import util.TextFormat;

import java.util.ArrayList;
import java.util.List;

public class InventoryReport implements Report {

    private final Inventory inventory;

    public InventoryReport(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String getTitle() {
        return "FULL INVENTORY REPORT";
    }

    @Override
    public String getFileName() {
        return "inventory_report.csv";
    }

    @Override
    public String[] getHeaders() {
        return new String[]{"ID", "Name", "Category", "Type", "Price", "Qty", "Stock Value", "Status"};
    }

    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        for (Product p : inventory.getProducts()) {
            rows.add(new String[]{
                    p.getId(), p.getName(), p.getCategory(), p.getTypeName(),
                    TextFormat.money(p.getPrice()), String.valueOf(p.getQuantity()),
                    TextFormat.money(p.getStockValue()), p.getStatus()});
        }
        return rows;
    }

    @Override
    public List<String[]> getTotals() {
        long units = 0;
        double value = 0;
        List<Product> products = inventory.getProducts();
        for (Product p : products) {
            units += p.getQuantity();
            value += p.getStockValue();
        }
        List<String[]> totals = new ArrayList<>();
        totals.add(new String[]{"TOTAL", products.size() + " products", "", "", "",
                String.valueOf(units), TextFormat.money(value), ""});
        return totals;
    }
}
