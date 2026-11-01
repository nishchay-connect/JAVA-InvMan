package report;

import model.TransactionType;
import service.Inventory;

public class RestockReport extends TransactionReport {

    public RestockReport(Inventory inventory) {
        super(inventory, TransactionType.RESTOCK, "RESTOCK REPORT", "restock_report.csv");
    }
}
