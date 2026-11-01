package report;

import model.StockTransaction;
import model.TransactionType;
import service.Inventory;
import util.TextFormat;

import java.util.ArrayList;
import java.util.List;


public abstract class TransactionReport implements Report {

    private final Inventory inventory;
    private final TransactionType type;
    private final String title;
    private final String fileName;

    protected TransactionReport(Inventory inventory, TransactionType type, String title, String fileName) {
        this.inventory = inventory;
        this.type = type;
        this.title = title;
        this.fileName = fileName;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String[] getHeaders() {
        return new String[]{"Txn ID", "Date/Time", "Product ID", "Product", "Qty", "Unit Price", "Total"};
    }

    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        for (StockTransaction t : inventory.getTransactions(type)) {
            rows.add(new String[]{
                    t.getId(), t.getTimestampText(), t.getProductId(), t.getProductName(),
                    String.valueOf(t.getQuantity()), TextFormat.money(t.getUnitPrice()),
                    TextFormat.money(t.getTotal())});
        }
        return rows;
    }

    @Override
    public List<String[]> getTotals() {
        long units = 0;
        double amount = 0;
        List<StockTransaction> list = inventory.getTransactions(type);
        for (StockTransaction t : list) {
            units += t.getQuantity();
            amount += t.getTotal();
        }
        List<String[]> totals = new ArrayList<>();
        totals.add(new String[]{"TOTAL", list.size() + " transactions", "", "",
                String.valueOf(units), "", TextFormat.money(amount)});
        return totals;
    }
}
