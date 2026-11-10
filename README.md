# Financial Portfolio Engine

Java application that manages a portfolio of financial assets (stocks, cryptocurrencies, bonds).

## 1. Purpose

The application tracks a portfolio made of several asset classes, values it using market prices, simulates price changes day after day and places buy and sell orders.

Each asset type has its own business rules: transaction fees, capital gains tax rate, allowed order size and annual income (dividends, coupons). A purchase recalculates the average purchase price, and a sale deducts fees and capital gains tax before crediting the cash balance.

At startup, the portfolio and the market are loaded from two CSV files. No business object is hard-coded.

## 2. How to run

### Requirements
- Java 25 or later
- Maven 3.9+

### Run the tests
```bash
mvn test
```

### Build and run the application
Run these commands from the project root (the data files are read from `src/main/resources/`).

```bash
mvn clean package
java -jar target/financial-portfolio-engine-0.1.jar
```

An optional web dashboard is also available:
```bash
java -jar target/financial-portfolio-engine-0.1.jar --web
```
then open `http://localhost:7070`. The console mode is the main mode.

## 3. Features

- Loads the initial portfolio (`assets.csv`, including cash) and the market (`market.csv`) at startup
- Shows positions: quantity, average purchase price, market price, value, unrealized gain/loss, ROI, portfolio weight and price trend
- Shows the market with the fee for one unit of each asset
- Buys an asset already owned (average purchase price is recalculated) or a new asset listed on the market
- Sells with fees and capital gains tax deducted, and removes the position when everything is sold
- Checks orders: enough cash, quantity valid for the asset type, quantity owned
- Estimates the portfolio's annual income (dividends and coupons)
- Simulates a market day (random change from -4 % to +4 % for each asset)
- Saves the portfolio state automatically after each order and on exit, and restores it on the next start
- Resets the portfolio to its initial state

## 4. Main classes

Package `com.junia.portfolio`:

| Package | Class | Role |
| --- | --- | --- |
| `model` | `Asset` | Abstract class for an asset: symbol, quantity, average purchase price, shared rules (value, capital gain, adding/removing units) |
| `model` | `AssetType`, `AssetCreator` | Enum of asset types; each value knows how to create its asset through the `AssetCreator` interface |
| `model` | `Stock`, `CryptoAsset`, `Bond` | Concrete asset types with their own fees, tax rate, order size and income |
| `model` | `Taxable`, `Tradeable` | Interfaces: capital gains taxation and order size validation |
| `model` | `Portfolio` | Owned positions and cash balance |
| `model` | `Order`, `OrderType`, `TradeReceipt` | A buy/sell order and the details of its execution (fees, tax, net amount) |
| `model` | `PositionSummary` | Calculated view of a position, used by the console and the web dashboard |
| `service` | `PortfolioService` | Order execution and transaction rules |
| `service` | `MarketDataService` | Listed assets, current prices, history and simulation |
| `service` | `AnalyticsService` | Valuation, gains/losses, ROI, weights, annual income, tax estimate |
| `utils` | `AbstractCsvParser<T>`, `AssetCsvParser`, `MarketCsvParser` | Reading CSV files and creating objects |
| `repository` | `PortfolioRepository`, `CsvPortfolioRepository` | Portfolio persistence interface and its CSV implementation |
| `utils` | `FinancialFormatter` | Allocation bars and sparklines in the console |
| `ui.console` | `ConsoleMenu` | User interaction in the console |
| `ui.web` | `PortfolioWebController` | REST API for the web dashboard (optional) |
| | `Application` | Entry point: wires the objects together and starts the console or the web dashboard |

## 5. Technical decisions

1. **Orders are executed at the market price.** The user picks the symbol and the quantity, and the price comes from `MarketDataService`. If users could type their own price, they could buy at 1 € and sell at the real price.

2. **A market file separate from the portfolio.** To buy an asset you don't own yet, you need its type and its parameters (dividend, network fee, coupon). `market.csv` lists the available assets, and a purchase calls `createPosition` on the listed asset to create a new position of the right type, without any `switch` or `instanceof` in the service.

3. **Asset types are described by an enum (`AssetType`).** Each enum value says how many specific parameters the asset expects and how to create it (a lambda implementing `AssetCreator`). The parser calls `AssetType.valueOf(type).create(...)`, so there is no `switch` on the type when reading or saving. Assets only expose their data (`getAssetType`, `getSpecificParameters`); `CsvPortfolioRepository` decides the file format.

4. **Checked exceptions for business and data errors.** Expected errors (insufficient funds, unknown asset, invalid order, malformed file) extend `Exception`, which forces the caller to handle them. Programming errors (an asset created with a negative quantity) throw an `IllegalArgumentException`.

5. **Numbers are written independently of the system language.** On a French system, `String.format("%.2f")` writes `2500,00`, which would break a comma-separated CSV. Saving uses `BigDecimal.toPlainString()`, which always writes a dot.

## 6. Inheritance

`Asset` is the parent class of `Stock`, `CryptoAsset` and `Bond`. It holds what all assets have in common: symbol, quantity, average purchase price, market value, taxable gain calculation, average price update on a purchase and unit removal on a sale.

`MarketCsvParser` extends `AssetCsvParser`: it reads a different column layout (price instead of quantity and purchase price) but reuses the asset creation.

## 7. Polymorphism

The services only work with `Asset`; the behavior depends on the actual type of the object:

| Method | `Stock` | `CryptoAsset` | `Bond` |
| --- | --- | --- | --- |
| `calculateTransactionFee` | 1 € + 0.1 % of the amount | network fee + 0.5 % | flat 5 € |
| `calculateAnnualIncome` | dividend | 0 | coupon on a 1000 € face value |
| `getTaxRate` | 30 % | 30 % | 15 % |
| `isOrderSizeValid` | whole number | down to 0.00000001 | whole number, at least 1 |
| `createPosition` | creates a `Stock` | creates a `CryptoAsset` | creates a `Bond` |
| `getSpecificParameters` | dividend rate | network fee | coupon rate, maturity year |

Example in `AnalyticsService`, without any type check:

```java
portfolio.getAssets().stream()
        .mapToDouble(asset -> asset.calculateAnnualIncome(marketDataService.getCurrentPrice(asset)))
        .sum();
```

And in `PortfolioService` during a purchase:

```java
double fee = reference.calculateTransactionFee(order.getQuantity(), order.getPrice());
```

## 8. Abstract class and interfaces

- **`Asset` (abstract class)**: a "generic" asset does not exist, so it must not be instantiated. The class holds both state and shared code (average price, capital gain), which an interface cannot do, and forces subclasses to implement the methods specific to each type.
- **`Taxable` (interface)**: describes the ability to be taxed on a capital gain (`calculateTaxableGain`, `getTaxRate`).
- **`Tradeable` (interface)**: describes the order size rule (`isOrderSizeValid`).
- **`AssetCreator` (interface)**: a single `create` method, implemented by a lambda in each `AssetType` value.
- **`PortfolioRepository` (interface)**: what the application needs to load and save a portfolio, without knowing that it is a CSV file.

The two interfaces are small and separate: each one represents a single capability. `Asset` implements both because, in this domain, every asset can be traded and is taxed on its gains. The capital gain calculation is shared and written in `Asset`; the tax rate and the order size rule are defined by each subclass.

## 9. Custom exceptions

Package `com.junia.portfolio.exception`, all extend `Exception`:

- `InsufficientFundsException`: the cost of a purchase, fees included, is higher than the available cash.
- `AssetNotFoundException`: selling an asset that is not owned, or buying a symbol that is not on the market.
- `InvalidTransactionException`: zero or negative quantity or price, quantity not allowed for this asset type (e.g. 2.5 shares), selling more units than owned, sale that does not cover the fees.
- `InvalidDataFileException`: invalid line in a file (column count, badly written number, unknown asset type, wrong number of parameters, negative data, duplicate symbol). The message gives the file and the line.

The standard exceptions used are `IOException` (missing or unreadable file), `NumberFormatException` (user input) and `IllegalArgumentException` (invalid data when building an object).

## 10. Data loading

Two files are read at startup from `src/main/resources/`:

- `market.csv`: listed assets and their starting price (`TYPE,SYMBOL,PRICE,EXTRA_PARAM,EXTRA_PARAM2`), read by `MarketCsvParser`.
- `assets.csv`: initial portfolio, with a `CASH` line for the cash balance (`TYPE,SYMBOL,QUANTITY,PURCHASE_PRICE,EXTRA_PARAM,EXTRA_PARAM2`), read by `CsvPortfolioRepository` and `AssetCsvParser`.

`AbstractCsvParser<T>` reads the file with `Files.lines()`, skips the header with `.skip(1)` and blank lines, then hands each line to the abstract `parseLine` method (Template Method pattern). The `TYPE` column is converted to an `AssetType`, which creates the right object (`Stock`, `CryptoAsset` or `Bond`).

If a saved state exists (`data/portfolio_state.csv`, same format as `assets.csv`), it is loaded instead of the initial portfolio. If it is corrupted, the application says so and starts from the initial portfolio. Market prices are not saved: they start again from `market.csv` on every launch.

## 11. SOLID principles

- **Single Responsibility**: `ConsoleMenu` only handles user interaction, `PortfolioService` the orders, `AnalyticsService` the indicators, `MarketDataService` the prices, `CsvPortfolioRepository` the persistence and the parsers the file reading. The model classes don't know the file format: if the save format changes, only the repository changes. `Application` only wires the objects together.
- **Open/Closed**: adding an asset type (e.g. `RealEstateAsset`) means creating a subclass of `Asset` and a value in `AssetType`. The parser, the repository, `PortfolioService` and `AnalyticsService` don't change: there is no `if` or `switch` on the asset type, and fees, income, tax and order size are methods of the asset.
- **Liskov Substitution**: any subclass of `Asset` can be used where an `Asset` is expected. The tests call the same methods on a `List<Asset>` containing all three types.
- **Interface Segregation**: `Taxable` and `Tradeable` are two short interfaces rather than one interface grouping unrelated methods.
- **Dependency Inversion**: dependencies are passed through constructors. `ConsoleMenu` and `PortfolioWebController` depend on the `PortfolioRepository` interface and not on `CsvPortfolioRepository`: only `Application` picks the implementation, so we could switch to a database without touching the user interface. `MarketDataService` receives its `Random`, which makes the simulation reproducible in tests.

## 12. Tests

The JUnit 5 tests are in `src/test/java/com/junia/portfolio` and run with `mvn test`. They cover normal cases, edge cases and error cases:

- `model.AssetTest`: fees, income and taxation by asset type (including a polymorphic test on a `List<Asset>`), order size, average purchase price, position creation, creation through `AssetType`, data validation.
- `model.PortfolioTest`: lookup, duplicate symbol, list that cannot be modified from outside, negative cash.
- `service.PortfolioServiceTest`: buying an owned or new asset, fees, insufficient funds (including because of fees), buying with exactly the whole balance, invalid quantities, selling with and without tax, full sale, impossible sale.
- `service.AnalyticsServiceTest`: total value, gains/losses, ROI, annual income, tax estimate, weights, position summary.
- `service.MarketDataServiceTest`: initial prices, history, invalid price, bounded daily change.
- `utils.AssetCsvParserTest` and `utils.MarketCsvParserTest`: correct reading, blank lines, invalid number, unknown type, missing columns, missing file.
- `repository.CsvPortfolioRepositoryTest`: saving and reloading every field, line format, French locale, reset, corrupted file without losing the portfolio in memory.
- `utils.FinancialFormatterTest`: bars and sparklines, including negative values.
- `EndToEndPortfolioFlowTest`: full scenario using the real files (buying a new asset, simulation, full sale, save and restore).
