package report;

import model.Product;
import service.Inventory;

import java.util.ArrayList;
import java.util.List;

public class LowStockReport implements Report {

    private final Inventory inventory;

    public LowStockReport(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String getTitle() {
        return "LOW STOCK REPORT";
    }

    @Override
    public String getFileName() {
        return "low_stock_report.csv";
    }

    @Override
    public String[] getHeaders() {
        return new String[]{"ID", "Name", "Category", "Qty", "Min Stock", "Status"};
    }

    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        for (Product p : inventory.getLowStockProducts()) {
            rows.add(new String[]{p.getId(), p.getName(), p.getCategory(),
                    String.valueOf(p.getQuantity()), String.valueOf(p.getMinStock()), p.getStatus()});
        }
        return rows;
    }

    @Override
    public List<String[]> getTotals() {
        List<String[]> totals = new ArrayList<>();
        totals.add(new String[]{"TOTAL", inventory.getLowStockProducts().size() + " product(s) need restocking"});
        return totals;
    }
}
