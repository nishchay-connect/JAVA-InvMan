package service;

import exception.DuplicateProductException;
import exception.InsufficientStockException;
import exception.InvalidProductDataException;
import exception.ProductNotFoundException;
import model.Product;
import model.StockTransaction;
import model.TransactionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;


public class Inventory {

    private final FileStorage storage;
    private final Map<String, Product> products = new TreeMap<>(); // sorted by ID
    private final List<StockTransaction> transactions = new ArrayList<>();
    private int txnCounter = 0;

    public Inventory(FileStorage storage) {
        this.storage = storage;
    }

    // Loading

    public void load(List<String> warnings) throws IOException {
        products.clear();
        transactions.clear();
        txnCounter = 0;

        for (Product p : storage.loadProducts(warnings)) {
            if (products.containsKey(p.getId())) {
                warnings.add("products.csv: duplicate product ID '" + p.getId() + "' (later row skipped)");
            } else {
                products.put(p.getId(), p);
            }
        }

        List<String> seenIds = new ArrayList<>();
        for (StockTransaction t : storage.loadTransactions(warnings)) {
            if (seenIds.contains(t.getId())) {
                warnings.add("transactions.csv: duplicate transaction ID '" + t.getId() + "' (later row skipped)");
                continue;
            }
            seenIds.add(t.getId());
            transactions.add(t);
            txnCounter = Math.max(txnCounter, t.getNumber());
        }
    }

    // Product management

    public void addProduct(Product p) throws DuplicateProductException, IOException {
        if (products.containsKey(p.getId())) {
            throw new DuplicateProductException(p.getId());
        }
        products.put(p.getId(), p);

        StockTransaction opening = null;
        if (p.getQuantity() > 0) {
            opening = new StockTransaction(nextTransactionId(), p.getId(), p.getName(),
                    TransactionType.RESTOCK, p.getQuantity(), p.getPrice());
            transactions.add(opening);
        }
        try {
            persist();
        } catch (IOException e) {
            products.remove(p.getId());
            if (opening != null) {
                transactions.remove(opening);
                txnCounter--;
            }
            rollbackSave();
            throw e;
        }
    }

    public void editProduct(String id, String name, String category, Double price,
                            Integer minStock, String extra)
            throws ProductNotFoundException, IOException {
        Product p = findById(id);
        String oldName = p.getName();
        String oldCategory = p.getCategory();
        double oldPrice = p.getPrice();
        int oldMin = p.getMinStock();
        String oldExtra = p.getExtra();

        try {
            if (name != null) p.setName(name);
            if (category != null) p.setCategory(category);
            if (price != null) p.setPrice(price);
            if (minStock != null) p.setMinStock(minStock);
            if (extra != null) p.setExtra(extra);
        } catch (InvalidProductDataException e) {
            restore(p, oldName, oldCategory, oldPrice, oldMin, oldExtra);
            throw e;
        }

        try {
            persist();
        } catch (IOException e) {
            restore(p, oldName, oldCategory, oldPrice, oldMin, oldExtra);
            rollbackSave();
            throw e;
        }
    }

    private void restore(Product p, String name, String category, double price, int min, String extra) {
        p.setName(name);
        p.setCategory(category);
        p.setPrice(price);
        p.setMinStock(min);
        p.setExtra(extra);
    }

    public Product removeProduct(String id) throws ProductNotFoundException, IOException {
        Product p = findById(id);
        if (p.getQuantity() > 0) {
            throw new InvalidProductDataException("Cannot delete '" + p.getName() + "': "
                    + p.getQuantity() + " unit(s) are still in stock.");
        }
        products.remove(p.getId());
        try {
            persist();
        } catch (IOException e) {
            products.put(p.getId(), p);
            rollbackSave();
            throw e;
        }
        return p;
    }

    // Stock operations

    public StockTransaction sell(String id, int quantity)
            throws ProductNotFoundException, InsufficientStockException, IOException {
        Product p = findById(id);
        p.decreaseStock(quantity); // validates quantity and checks availability
        StockTransaction t = new StockTransaction(nextTransactionId(), p.getId(), p.getName(),
                TransactionType.SALE, quantity, p.getPrice());
        transactions.add(t);
        try {
            persist();
        } catch (IOException e) {
            transactions.remove(t);
            txnCounter--;
            p.increaseStock(quantity);
            rollbackSave();
            throw e;
        }
        return t;
    }

    public StockTransaction restock(String id, int quantity)
            throws ProductNotFoundException, IOException {
        Product p = findById(id);
        p.increaseStock(quantity); // validates quantity and the maximum stock limit
        StockTransaction t = new StockTransaction(nextTransactionId(), p.getId(), p.getName(),
                TransactionType.RESTOCK, quantity, p.getPrice());
        transactions.add(t);
        try {
            persist();
        } catch (IOException e) {
            transactions.remove(t);
            txnCounter--;
            try {
                p.decreaseStock(quantity);
            } catch (InsufficientStockException impossible) {
                // we just added these units, so they are certainly available
            }
            rollbackSave();
            throw e;
        }
        return t;
    }

    // Queries

    public boolean exists(String id) {
        return products.containsKey(normalise(id));
    }

    public Product findById(String id) throws ProductNotFoundException {
        Product p = products.get(normalise(id));
        if (p == null) {
            throw new ProductNotFoundException(id == null ? "" : id.trim());
        }
        return p;
    }

    public List<Product> search(String query) {
        List<Product> result = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return result;
        }
        Product exact = products.get(normalise(q));
        if (exact != null) {
            result.add(exact);
            return result;
        }
        String lower = q.toLowerCase(Locale.ROOT);
        for (Product p : products.values()) {
            if (p.getId().toLowerCase(Locale.ROOT).contains(lower)
                    || p.getName().toLowerCase(Locale.ROOT).contains(lower)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<String> getCategories() {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Product p : products.values()) {
            set.add(p.getCategory());
        }
        return new ArrayList<>(set);
    }

    public List<Product> getByCategory(String category) {
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.getCategory().equalsIgnoreCase(category)) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> getLowStockProducts() {
        List<Product> result = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.isLowStock()) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> getProducts() {
        return new ArrayList<>(products.values());
    }

    public List<StockTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public List<StockTransaction> getTransactions(TransactionType type) {
        List<StockTransaction> result = new ArrayList<>();
        for (StockTransaction t : transactions) {
            if (t.getType() == type) {
                result.add(t);
            }
        }
        return result;
    }

    public List<StockTransaction> getTransactionsFor(String productId) {
        List<StockTransaction> result = new ArrayList<>();
        for (StockTransaction t : transactions) {
            if (t.getProductId().equalsIgnoreCase(productId)) {
                result.add(t);
            }
        }
        return result;
    }

    private String normalise(String id) {
        return id == null ? "" : id.trim().toUpperCase(Locale.ROOT);
    }

    private String nextTransactionId() {
        txnCounter++;
        return String.format("T%03d", txnCounter);
    }

    private void persist() throws IOException {
        storage.saveTransactions(transactions);
        storage.saveProducts(products.values());
    }

    
    private void rollbackSave() {
        try {
            persist();
        } catch (IOException ignored) {
        }
    }
}
