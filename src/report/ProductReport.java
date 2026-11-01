package report;

import model.Product;
import model.StockTransaction;
import model.TransactionType;
import service.Inventory;
import util.TextFormat;

import java.util.ArrayList;
import java.util.List;

public class ProductReport implements Report {

    private final Inventory inventory;
    private final Product product;

    public ProductReport(Inventory inventory, Product product) {
        this.inventory = inventory;
        this.product = product;
    }

    @Override
    public String getTitle() {
        return "PRODUCT REPORT: " + product.getId() + " - " + product.getName();
    }

    @Override
    public String getFileName() {
        return "product_" + product.getId() + "_report.csv";
    }

    @Override
    public String[] getHeaders() {
        return new String[]{"Txn ID", "Date/Time", "Type", "Qty", "Unit Price", "Total"};
    }

    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        for (StockTransaction t : inventory.getTransactionsFor(product.getId())) {
            rows.add(new String[]{
                    t.getId(), t.getTimestampText(), t.getType().name(),
                    String.valueOf(t.getQuantity()), TextFormat.money(t.getUnitPrice()),
                    TextFormat.money(t.getTotal())});
        }
        return rows;
    }

    @Override
    public List<String[]> getTotals() {
        long soldUnits = 0, restockedUnits = 0;
        double revenue = 0, restockValue = 0;
        int soldCount = 0, restockCount = 0;
        for (StockTransaction t : inventory.getTransactionsFor(product.getId())) {
            if (t.getType() == TransactionType.SALE) {
                soldUnits += t.getQuantity();
                revenue += t.getTotal();
                soldCount++;
            } else {
                restockedUnits += t.getQuantity();
                restockValue += t.getTotal();
                restockCount++;
            }
        }
        List<String[]> totals = new ArrayList<>();
        totals.add(new String[]{"", "TOTAL SOLD (" + soldCount + " txns)", "",
                String.valueOf(soldUnits), "", TextFormat.money(revenue)});
        totals.add(new String[]{"", "TOTAL RESTOCKED (" + restockCount + " txns)", "",
                String.valueOf(restockedUnits), "", TextFormat.money(restockValue)});
        totals.add(new String[]{"", "CURRENT STOCK", "",
                String.valueOf(product.getQuantity()), "", ""});
        return totals;
    }
}
