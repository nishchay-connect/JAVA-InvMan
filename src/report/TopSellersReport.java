package report;

import model.StockTransaction;
import model.TransactionType;
import service.Inventory;
import util.TextFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TopSellersReport implements Report {

    private final Inventory inventory;

    public TopSellersReport(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String getTitle() {
        return "TOP SELLING PRODUCTS";
    }

    @Override
    public String getFileName() {
        return "top_sellers_report.csv";
    }

    @Override
    public String[] getHeaders() {
        return new String[]{"Rank", "Product ID", "Product", "Units Sold", "Revenue"};
    }

    private List<Object[]> summarise() {
        Map<String, Object[]> byProduct = new LinkedHashMap<>(); // id -> {name, units, revenue}
        for (StockTransaction t : inventory.getTransactions(TransactionType.SALE)) {
            Object[] entry = byProduct.computeIfAbsent(t.getProductId(),
                    k -> new Object[]{t.getProductName(), 0L, 0.0});
            entry[0] = t.getProductName();
            entry[1] = (Long) entry[1] + t.getQuantity();
            entry[2] = (Double) entry[2] + t.getTotal();
        }
        List<Object[]> list = new ArrayList<>();
        for (Map.Entry<String, Object[]> e : byProduct.entrySet()) {
            list.add(new Object[]{e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]});
        }
        list.sort((a, b) -> Long.compare((Long) b[2], (Long) a[2]));
        return list;
    }

    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] s : summarise()) {
            rows.add(new String[]{String.valueOf(rank++), (String) s[0], (String) s[1],
                    String.valueOf(s[2]), TextFormat.money((Double) s[3])});
        }
        return rows;
    }

    @Override
    public List<String[]> getTotals() {
        long units = 0;
        double revenue = 0;
        for (Object[] s : summarise()) {
            units += (Long) s[2];
            revenue += (Double) s[3];
        }
        List<String[]> totals = new ArrayList<>();
        totals.add(new String[]{"TOTAL", "", "", String.valueOf(units), TextFormat.money(revenue)});
        return totals;
    }
}
