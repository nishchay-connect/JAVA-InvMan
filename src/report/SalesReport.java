package report;

import model.TransactionType;
import service.Inventory;

public class SalesReport extends TransactionReport {

    public SalesReport(Inventory inventory) {
        super(inventory, TransactionType.SALE, "SALES REPORT", "sales_report.csv");
    }
}
