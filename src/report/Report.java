package report;

import model.StockTransaction;
import util.TextFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public interface Report {

    String getTitle();

    String getFileName();

    String[] getHeaders();

    List<String[]> getRows();

    List<String[]> getTotals();

    default String toText() {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(getTitle()).append(nl);
        sb.append("Generated: ").append(LocalDateTime.now().format(StockTransaction.FORMAT)).append(nl);
        sb.append(nl);
        List<String[]> rows = getRows();
        sb.append(TextFormat.table(getHeaders(), rows, getTotals()));
        if (rows.isEmpty()) {
            sb.append("(no records)").append(nl);
        }
        return sb.toString();
    }

    default List<String> toCsvLines() {
        List<String> lines = new ArrayList<>();
        lines.add(csvLine(getHeaders()));
        for (String[] row : getRows()) {
            lines.add(csvLine(row));
        }
        for (String[] row : getTotals()) {
            lines.add(csvLine(row));
        }
        return lines;
    }

    private static String csvLine(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String c = cells[i] == null ? "" : cells[i];
            if (c.contains(",") || c.contains("\"") || c.contains("\n")) {
                c = "\"" + c.replace("\"", "\"\"") + "\"";
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
