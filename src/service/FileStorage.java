package service;

import exception.InvalidProductDataException;
import model.ElectronicsProduct;
import model.GeneralProduct;
import model.GroceryProduct;
import model.Product;
import model.StockTransaction;
import model.TransactionType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;


public class FileStorage {

    private static final String PRODUCT_HEADER =
            "type,id,name,category,price,quantity,minStock,extra";
    private static final String TRANSACTION_HEADER =
            "id,productId,productName,type,quantity,unitPrice,total,timestamp";

    private final Path dataDir;
    private final Path reportsDir;
    private final Path productsFile;
    private final Path transactionsFile;

    public FileStorage(String dataDirName, String reportsDirName) {
        this.dataDir = Paths.get(dataDirName);
        this.reportsDir = Paths.get(reportsDirName);
        this.productsFile = dataDir.resolve("products.csv");
        this.transactionsFile = dataDir.resolve("transactions.csv");
    }

    // Startup

    public void ensureFiles() throws IOException {
        Files.createDirectories(dataDir);
        if (!Files.exists(productsFile)) {
            writeLines(productsFile, List.of(PRODUCT_HEADER));
        }
        if (!Files.exists(transactionsFile)) {
            writeLines(transactionsFile, List.of(TRANSACTION_HEADER));
        }
    }

    public Path backupDataFiles() throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupDir = dataDir.resolve("backup_" + stamp);
        Files.createDirectories(backupDir);
        for (Path file : new Path[]{productsFile, transactionsFile}) {
            if (Files.exists(file)) {
                Files.copy(file, backupDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return backupDir;
    }

    
    public List<Product> loadProducts(List<String> warnings) throws IOException {
        List<Product> result = new ArrayList<>();
        List<String> lines = readLines(productsFile);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            if (i == 0 && line.toLowerCase(Locale.ROOT).startsWith("type,")) {
                continue; // header row
            }
            try {
                result.add(parseProduct(line));
            } catch (RuntimeException e) {
                warnings.add("products.csv line " + (i + 1) + ": " + e.getMessage() + " (row skipped)");
            }
        }
        return result;
    }

    public List<StockTransaction> loadTransactions(List<String> warnings) throws IOException {
        List<StockTransaction> result = new ArrayList<>();
        List<String> lines = readLines(transactionsFile);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            if (i == 0 && line.toLowerCase(Locale.ROOT).startsWith("id,")) {
                continue; // header row
            }
            try {
                result.add(parseTransaction(line));
            } catch (RuntimeException e) {
                warnings.add("transactions.csv line " + (i + 1) + ": " + e.getMessage() + " (row skipped)");
            }
        }
        return result;
    }

    private Product parseProduct(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 8) {
            throw new InvalidProductDataException("expected 8 columns but found " + f.length);
        }
        String type = f[0].trim().toUpperCase(Locale.ROOT);
        double price = Product.parsePrice(f[4]);
        int quantity = Product.parseQuantity(f[5]);
        int minStock = Product.parseMinStock(f[6]);

        switch (type) {
            case "GENERAL":
                return new GeneralProduct(f[1], f[2], f[3], price, quantity, minStock);
            case "ELECTRONICS":
                return new ElectronicsProduct(f[1], f[2], f[3], price, quantity, minStock,
                        ElectronicsProduct.parseWarranty(f[7]));
            case "GROCERY":
                return new GroceryProduct(f[1], f[2], f[3], price, quantity, minStock,
                        GroceryProduct.parseDate(f[7]));
            default:
                throw new InvalidProductDataException("unknown product type '" + f[0].trim() + "'");
        }
    }

    private StockTransaction parseTransaction(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 8) {
            throw new InvalidProductDataException("expected 8 columns but found " + f.length);
        }
        String id = f[0].trim();
        if (!id.matches("T\\d{1,9}")) {
            throw new InvalidProductDataException("invalid transaction ID '" + id + "'");
        }
        TransactionType type;
        try {
            type = TransactionType.valueOf(f[3].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidProductDataException("unknown transaction type '" + f[3].trim() + "'");
        }
        LocalDateTime time;
        try {
            time = LocalDateTime.parse(f[7].trim(), StockTransaction.FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidProductDataException("invalid timestamp '" + f[7].trim() + "'");
        }
        return new StockTransaction(id, Product.validateId(f[1]), Product.validateName(f[2]),
                type, Product.parseTransactionQuantity(f[4]), Product.parsePrice(f[5]), time);
    }

  
    public void saveProducts(Collection<Product> products) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(PRODUCT_HEADER);
        for (Product p : products) {
            lines.add(p.toCsvRow());
        }
        writeLines(productsFile, lines);
    }

    public void saveTransactions(Collection<StockTransaction> transactions) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(TRANSACTION_HEADER);
        for (StockTransaction t : transactions) {
            lines.add(t.toCsvRow());
        }
        writeLines(transactionsFile, lines);
    }

    public Path saveReport(String fileName, List<String> lines) throws IOException {
        Path target = reportsDir.resolve(fileName);
        writeLines(target, lines);
        return target;
    }

    private List<String> readLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        if (!lines.isEmpty() && lines.get(0).startsWith("﻿")) {
            lines.set(0, lines.get(0).substring(1)); // strip BOM added by some editors/Excel
        }
        return lines;
    }

    private void writeLines(Path target, List<String> lines) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path temp = dir.resolve(target.getFileName() + ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
            throw e;
        }
    }
}
