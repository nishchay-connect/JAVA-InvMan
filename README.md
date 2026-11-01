# OOPS 3RD SEMESTER PROJECT

## Inventory Management System (Java, terminal based)

A small console application for a shop: add / edit / delete products, record
sales and restocks, get low-stock alerts, and generate reports. Data is stored
in CSV files. 

## Compile and run

Run these from the project folder (the one that contains `src/`):

```
javac -d out -sourcepath src src/Main.java
java -cp out Main
```

Needs JDK 17 or newer. On first run the `data/` folder and both CSV files are
created automatically. Reports are written to `reports/`.

**Demo data:** copy the two files from `sample_data/` into `data/` before
starting (6 products, a few sales, two items already low on stock).

## Menus

```
MAIN MENU
 1. Product Management      add / view / edit / delete
 2. Stock Management        sell (stock out) / restock (stock in)
 3. Transaction History     all / sales / restocks / one product
 4. Search Products         by ID or name / filter by category
 5. Reports                 6 reports, shown on screen AND saved as CSV
 6. Low Stock Alerts
```

Anywhere a product is needed you can type its ID *or* (part of) its name. If
several products match, you pick one by its list number.

## Project structure

```
src/
  Main.java                 menus and navigation only
  model/                    Product (abstract), ElectronicsProduct, GroceryProduct,
                            GeneralProduct, StockTransaction, TransactionType (enum)
  exception/                ProductNotFound, DuplicateProduct, InsufficientStock,
                            InvalidProductData
  service/                  Inventory (business logic), FileStorage (all CSV I/O)
  report/                   Report (interface), InventoryReport, TransactionReport (abstract),
                            SalesReport, RestockReport, ProductReport, TopSellersReport,
                            LowStockReport
  ui/                       InputHelper (safe console input)
  util/                     TextFormat (tables, number formatting)
```


## Data files

`data/products.csv`
`type,id,name,category,price,quantity,minStock,extra`
(`extra` = warranty months for ELECTRONICS, expiry date `yyyy-MM-dd` for GROCERY, empty for GENERAL)

`data/transactions.csv`
`id,productId,productName,type,quantity,unitPrice,total,timestamp`

Each transaction stores the unit price and product name *at that moment*, so
editing a price or deleting a product never changes history.

## Error handling

| Situation | What happens |
|---|---|
| Invalid menu choice / non-numeric input | Message, asks again |
| Empty name, comma in name, negative or zero price, bad quantity, bad date | Rejected with a reason, asks again |
| Duplicate product ID | Rejected |
| Product not found | Message, back to menu |
| Sale larger than stock | `InsufficientStockException`, nothing changes |
| Delete a product that still has stock | Refused |
| Missing data files | Created automatically |
| Data file cannot be read at startup | Program stops instead of risking overwriting your data |
| Disk write fails (file open in Excel, read-only, ...) | Error shown, the change is undone in memory, nothing is reported as saved |
| Input stream closed (Ctrl+D / Ctrl+Z) | Clean exit (everything completed was already saved) |

Every change is saved immediately. Saving writes to `<file>.tmp` first and then
replaces the real file, so a crash never leaves a half-written CSV.

## Known simplifications (deliberate)

- A restock is recorded at the product's current price (there is no separate cost price).
- Single user, no login, no database, no GUI.

~ Nishchay 
2025BTCSE017