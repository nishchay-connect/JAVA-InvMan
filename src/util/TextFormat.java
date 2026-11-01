package util;

import java.util.List;
import java.util.Locale;

public final class TextFormat {

    private TextFormat() {
    }

    public static String money(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

  
    public static String table(String[] headers, List<String[]> rows, List<String[]> totals) {
        int cols = headers.length;
        int[] width = new int[cols];
        for (int c = 0; c < cols; c++) {
            width[c] = headers[c].length();
        }
        for (String[] row : rows) {
            widen(width, row);
        }
        for (String[] row : totals) {
            widen(width, row);
        }

        StringBuilder sb = new StringBuilder();
        appendRow(sb, headers, width);
        appendSeparator(sb, width);
        for (String[] row : rows) {
            appendRow(sb, row, width);
        }
        if (!totals.isEmpty()) {
            appendSeparator(sb, width);
            for (String[] row : totals) {
                appendRow(sb, row, width);
            }
        }
        return sb.toString();
    }

    private static void widen(int[] width, String[] row) {
        for (int c = 0; c < width.length; c++) {
            width[c] = Math.max(width[c], cell(row, c).length());
        }
    }

    private static String cell(String[] row, int c) {
        return (c < row.length && row[c] != null) ? row[c] : "";
    }

    private static void appendRow(StringBuilder sb, String[] row, int[] width) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < width.length; c++) {
            if (c > 0) {
                line.append(" | ");
            }
            line.append(String.format("%-" + width[c] + "s", cell(row, c)));
        }
        sb.append(line.toString().stripTrailing()).append(System.lineSeparator());
    }

    private static void appendSeparator(StringBuilder sb, int[] width) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < width.length; c++) {
            if (c > 0) {
                line.append("-+-");
            }
            line.append("-".repeat(width[c]));
        }
        sb.append(line).append(System.lineSeparator());
    }
}
