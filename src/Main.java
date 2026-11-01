import exception.DuplicateProductException;
import exception.InsufficientStockException;
import exception.InvalidProductDataException;
import exception.ProductNotFoundException;
import model.ElectronicsProduct;
import model.GeneralProduct;
import model.GroceryProduct;
import model.Product;
import model.StockTransaction;
import model.TransactionType;
import report.InventoryReport;
import report.LowStockReport;
import report.ProductReport;
import report.Report;
import report.RestockReport;
import report.SalesReport;
import report.TopSellersReport;
import service.FileStorage;
import service.Inventory;
import ui.InputHelper;
import util.TextFormat;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;


public class Main {

    private interface Action {
        void run() throws Exception;
    }

    private final InputHelper in = new InputHelper(new Scanner(System.in));
    private final FileStorage storage = new FileStorage("data", "reports");
    private final Inventory inventory = new Inventory(storage);

    public static void main(String[] args) {
        new Main().start();
    }

    private void start() {
        try {
            if (!startup()) {
                System.exit(1);
            }
            mainMenu();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Input closed - exiting. (Every completed operation was already saved.)");
        }
    }

    // Startup

    private boolean startup() {
        System.out.println("==========================================");
        System.out.println("     INVENTORY MANAGEMENT SYSTEM");
        System.out.println("==========================================");

        List<String> warnings = new ArrayList<>();
        try {
            storage.ensureFiles();
            inventory.load(warnings);
        } catch (IOException e) {
            System.out.println("FATAL: could not read the data files: " + e.getMessage());
            System.out.println("The program will not start, to avoid overwriting your data.");
            System.out.println("Fix the problem (file in use? permissions? damaged file?) and run again.");
            return false;
        }

        System.out.println("Loaded " + inventory.getProducts().size() + " product(s) and "
                + inventory.getTransactions().size() + " transaction(s).");

        if (!warnings.isEmpty()) {
            System.out.println();
            System.out.println("WARNING: " + warnings.size() + " problem(s) found in the data files:");
            for (String w : warnings) {
                System.out.println("  - " + w);
            }
            try {
                Path backup = storage.backupDataFiles();
                System.out.println("A copy of the original files was saved to: " + backup);
                System.out.println("(Skipped rows will disappear from the main files on the next save.)");
            } catch (IOException e) {
                System.out.println("Could not create a backup of the original files: " + e.getMessage());
                if (!in.confirm("Continue anyway?")) {
                    return false;
                }
            }
        }

        System.out.println();
        printLowStockAlerts();
        return true;
    }

    // Menus

    private void printMenu(String title, String zeroLabel, String... items) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        for (int i = 0; i < items.length; i++) {
            System.out.println(" " + (i + 1) + ". " + items[i]);
        }
        System.out.println(" 0. " + zeroLabel);
    }

    private void mainMenu() {
        while (true) {
            printMenu("MAIN MENU", "Exit",
                    "Product Management",
                    "Stock Management (Sell / Restock)",
                    "Transaction History",
                    "Search Products",
                    "Reports",
                    "Low Stock Alerts");
            switch (in.readInt("Choose an option: ", 0, 6)) {
                case 1: productMenu(); break;
                case 2: stockMenu(); break;
                case 3: historyMenu(); break;
                case 4: searchMenu(); break;
                case 5: reportsMenu(); break;
                case 6: printLowStockAlerts(); break;
                default:
                    System.out.println("Goodbye!");
                    return;
            }
        }
    }

    private void productMenu() {
        while (true) {
            printMenu("PRODUCT MANAGEMENT", "Back to main menu",
                    "Add product", "View all products", "Edit product", "Delete product");
            switch (in.readInt("Choose an option: ", 0, 4)) {
                case 1: run(this::addProduct); break;
                case 2: showProducts(inventory.getProducts()); break;
                case 3: run(this::editProduct); break;
                case 4: run(this::deleteProduct); break;
                default: return;
            }
        }
    }

    private void stockMenu() {
        while (true) {
            printMenu("STOCK MANAGEMENT", "Back to main menu",
                    "Sell product (stock out)", "Restock product (stock in)");
            switch (in.readInt("Choose an option: ", 0, 2)) {
                case 1: run(this::sellProduct); break;
                case 2: run(this::restockProduct); break;
                default: return;
            }
        }
    }

    private void historyMenu() {
        while (true) {
            printMenu("TRANSACTION HISTORY", "Back to main menu",
                    "All transactions", "Sales only", "Restocks only", "Transactions of one product");
            switch (in.readInt("Choose an option: ", 0, 4)) {
                case 1: showTransactions(inventory.getTransactions()); break;
                case 2: showTransactions(inventory.getTransactions(TransactionType.SALE)); break;
                case 3: showTransactions(inventory.getTransactions(TransactionType.RESTOCK)); break;
                case 4:
                    Product p = selectProduct("view history of");
                    if (p != null) {
                        showTransactions(inventory.getTransactionsFor(p.getId()));
                    }
                    break;
                default: return;
            }
        }
    }

    private void searchMenu() {
        while (true) {
            printMenu("SEARCH PRODUCTS", "Back to main menu",
                    "Search by product ID or name", "Filter by category");
            switch (in.readInt("Choose an option: ", 0, 2)) {
                case 1:
                    String q = in.readLine("Enter product ID or name (part of it is fine): ");
                    List<Product> found = inventory.search(q);
                    if (found.isEmpty()) {
                        System.out.println("No product matches '" + q + "'.");
                    } else {
                        showProducts(found);
                    }
                    break;
                case 2:
                    List<String> categories = inventory.getCategories();
                    if (categories.isEmpty()) {
                        System.out.println("There are no products yet.");
                        break;
                    }
                    for (int i = 0; i < categories.size(); i++) {
                        System.out.println(" " + (i + 1) + ". " + categories.get(i));
                    }
                    int n = in.readInt("Choose a category (0 to cancel): ", 0, categories.size());
                    if (n > 0) {
                        showProducts(inventory.getByCategory(categories.get(n - 1)));
                    }
                    break;
                default: return;
            }
        }
    }

    private void reportsMenu() {
        while (true) {
            printMenu("REPORTS", "Back to main menu",
                    "Full inventory report",
                    "Sales report",
                    "Restock report",
                    "Report for one product",
                    "Top selling products",
                    "Low stock report");
            switch (in.readInt("Choose an option: ", 0, 6)) {
                case 1: showReport(new InventoryReport(inventory)); break;
                case 2: showReport(new SalesReport(inventory)); break;
                case 3: showReport(new RestockReport(inventory)); break;
                case 4:
                    Product p = selectProduct("create a report for");
                    if (p != null) {
                        showReport(new ProductReport(inventory, p));
                    }
                    break;
                case 5: showReport(new TopSellersReport(inventory)); break;
                case 6: showReport(new LowStockReport(inventory)); break;
                default: return;
            }
        }
    }

    // Error handling wrapper

    private void run(Action action) {
        try {
            action.run();
        } catch (NoSuchElementException e) {
            throw e; 
        } catch (InsufficientStockException e) {
            System.out.println("ERROR: Sale rejected. " + e.getMessage());
        } catch (ProductNotFoundException | DuplicateProductException | InvalidProductDataException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR: Could not write to the data files (" + e.getMessage() + ").");
            System.out.println("The operation was NOT completed and nothing was changed.");
            System.out.println("Check that the files are not open in another program, then try again.");
        } catch (Exception e) {
            System.out.println("UNEXPECTED ERROR: " + e);
            System.out.println("Nothing was saved for this operation.");
        }
    }

    // Product management actions

    private void addProduct() throws DuplicateProductException, IOException {
        System.out.println();
        System.out.println("Product type:  1. General   2. Electronics (warranty)   3. Grocery (expiry date)   0. Cancel");
        int type = in.readInt("Choose type: ", 0, 3);
        if (type == 0) {
            return;
        }

        String id = in.readValid("Product ID (e.g. P101): ", Product::validateId);
        if (inventory.exists(id)) {
            System.out.println("ERROR: A product with ID '" + id + "' already exists.");
            return;
        }
        String name = in.readValid("Name: ", Product::validateName);
        String category = in.readValid("Category: ", Product::validateCategory);
        double price = in.readValid("Price: ", Product::parsePrice);
        int quantity = in.readValid("Opening stock quantity (0 is fine): ", Product::parseQuantity);
        int minStock = in.readValid("Minimum stock threshold: ", Product::parseMinStock);

        Product product;
        if (type == 2) {
            int warranty = in.readValid("Warranty (months): ", ElectronicsProduct::parseWarranty);
            product = new ElectronicsProduct(id, name, category, price, quantity, minStock, warranty);
        } else if (type == 3) {
            LocalDate expiry = in.readValid("Expiry date (yyyy-MM-dd): ", GroceryProduct::parseDate);
            product = new GroceryProduct(id, name, category, price, quantity, minStock, expiry);
        } else {
            product = new GeneralProduct(id, name, category, price, quantity, minStock);
        }

        inventory.addProduct(product);
        System.out.println("Product added: " + product.getId() + " - " + product.getName()
                + " (" + product.getTypeName() + ")");
        if (quantity > 0) {
            System.out.println("Opening stock of " + quantity + " was recorded as a restock transaction.");
        }
    }

    private void editProduct() throws ProductNotFoundException, IOException {
        Product p = selectProduct("edit");
        if (p == null) {
            return;
        }
        System.out.println("Editing " + p.getId() + " - press Enter to keep the current value.");
        System.out.println("(Stock quantity is changed through Stock Management, not here.)");

        String name = in.readValidOrBlank("Name [" + p.getName() + "]: ", Product::validateName);
        String category = in.readValidOrBlank("Category [" + p.getCategory() + "]: ", Product::validateCategory);
        Double price = in.readValidOrBlank("Price [" + TextFormat.money(p.getPrice()) + "]: ", Product::parsePrice);
        Integer minStock = in.readValidOrBlank("Minimum stock [" + p.getMinStock() + "]: ", Product::parseMinStock);
        String extra = null;
        if (!p.getExtraLabel().isEmpty()) {
            extra = in.readValidOrBlank(p.getExtraLabel() + " [" + p.getExtra() + "]: ", s -> {
                p.validateExtra(s);
                return s;
            });
        }

        if (name == null && category == null && price == null && minStock == null && extra == null) {
            System.out.println("Nothing was changed.");
            return;
        }
        inventory.editProduct(p.getId(), name, category, price, minStock, extra);
        System.out.println("Product updated. Past transactions keep the price they were made at.");
    }

    private void deleteProduct() throws ProductNotFoundException, IOException {
        Product p = selectProduct("delete");
        if (p == null) {
            return;
        }
        if (p.getQuantity() > 0) {
            System.out.println("ERROR: Cannot delete '" + p.getName() + "' - " + p.getQuantity()
                    + " unit(s) are still in stock. Sell the stock first.");
            return;
        }
        if (!in.confirm("Delete " + p.getId() + " - " + p.getName() + "? Its history is kept")) {
            System.out.println("Cancelled.");
            return;
        }
        inventory.removeProduct(p.getId());
        System.out.println("Product deleted.");
    }

    // Stock actions

    private void sellProduct() throws ProductNotFoundException, InsufficientStockException, IOException {
        Product p = selectProduct("sell");
        if (p == null) {
            return;
        }
        System.out.println("Price: " + TextFormat.money(p.getPrice()) + "   In stock: " + p.getQuantity());
        int qty = in.readValid("Quantity to sell: ", Product::parseTransactionQuantity);

        StockTransaction t = inventory.sell(p.getId(), qty);
        System.out.println("Sale recorded! (" + t.getId() + ")");
        System.out.println("  " + qty + " x " + p.getName() + " @ " + TextFormat.money(t.getUnitPrice())
                + " = " + TextFormat.money(t.getTotal()));
        System.out.println("  Remaining stock: " + p.getQuantity());
        if (p.isLowStock()) {
            System.out.println("[ALERT] " + p.getName() + " is now " + p.getStatus()
                    + " (" + p.getQuantity() + " left, minimum is " + p.getMinStock() + "). Consider restocking.");
        }
    }

    private void restockProduct() throws ProductNotFoundException, IOException {
        Product p = selectProduct("restock");
        if (p == null) {
            return;
        }
        System.out.println("Current stock: " + p.getQuantity());
        int qty = in.readValid("Quantity received: ", Product::parseTransactionQuantity);

        StockTransaction t = inventory.restock(p.getId(), qty);
        System.out.println("Restock recorded! (" + t.getId() + ")");
        System.out.println("  New stock of " + p.getName() + ": " + p.getQuantity());
    }


    private Product selectProduct(String purpose) {
        String query = in.readLine("Enter product ID or name to " + purpose + " (blank to cancel): ");
        if (query.isEmpty()) {
            return null;
        }
        List<Product> found = inventory.search(query);
        if (found.isEmpty()) {
            System.out.println("No product matches '" + query + "'.");
            return null;
        }
        if (found.size() == 1) {
            Product p = found.get(0);
            System.out.println("Selected: " + p.getId() + " - " + p.getName());
            return p;
        }
        System.out.println(found.size() + " products match:");
        showProducts(found);
        int n = in.readInt("Select a product number (0 to cancel): ", 0, found.size());
        return n == 0 ? null : found.get(n - 1);
    }

    
    private void showProducts(List<Product> list) {
        if (list.isEmpty()) {
            System.out.println("No products to show.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        int i = 1;
        for (Product p : list) {
            rows.add(new String[]{
                    String.valueOf(i++), p.getId(), p.getName(), p.getCategory(),
                    TextFormat.money(p.getPrice()), String.valueOf(p.getQuantity()),
                    String.valueOf(p.getMinStock()), p.getStatus(),
                    p.getDetails()}); // polymorphism: each product type describes itself
        }
        System.out.println();
        System.out.print(TextFormat.table(
                new String[]{"#", "ID", "Name", "Category", "Price", "Qty", "Min", "Status", "Details"},
                rows, new ArrayList<String[]>()));
    }

    private void showTransactions(List<StockTransaction> list) {
        if (list.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (StockTransaction t : list) {
            rows.add(new String[]{
                    t.getId(), t.getTimestampText(), t.getProductId(), t.getProductName(),
                    t.getType().name(), String.valueOf(t.getQuantity()),
                    TextFormat.money(t.getUnitPrice()), TextFormat.money(t.getTotal())});
        }
        System.out.println();
        System.out.print(TextFormat.table(
                new String[]{"Txn ID", "Date/Time", "Product ID", "Product", "Type", "Qty", "Unit Price", "Total"},
                rows, new ArrayList<String[]>()));
    }

    private void printLowStockAlerts() {
        List<Product> low = inventory.getLowStockProducts();
        if (low.isEmpty()) {
            System.out.println("Stock check: all products are above their minimum level.");
            return;
        }
        System.out.println("[ALERT] " + low.size() + " product(s) at or below minimum stock:");
        List<String[]> rows = new ArrayList<>();
        for (Product p : low) {
            rows.add(new String[]{p.getId(), p.getName(), String.valueOf(p.getQuantity()),
                    String.valueOf(p.getMinStock()), p.getStatus()});
        }
        System.out.print(TextFormat.table(
                new String[]{"ID", "Name", "Qty", "Min", "Status"}, rows, new ArrayList<String[]>()));
    }

    /** Shows a report on screen and also saves it as a CSV file. */
    private void showReport(Report report) {
        System.out.println();
        System.out.print(report.toText());
        try {
            Path saved = storage.saveReport(report.getFileName(), report.toCsvLines());
            System.out.println();
            System.out.println("Report saved to: " + saved);
        } catch (IOException e) {
            System.out.println();
            System.out.println("WARNING: the report is shown above but could not be saved to a file: "
                    + e.getMessage());
        }
    }
}
