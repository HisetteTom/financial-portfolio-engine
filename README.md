# Financial Portfolio Engine

A Java application that manages a portfolio of stocks, cryptocurrencies and bonds.

## 1. Purpose

The application keeps track of a portfolio with several kinds of assets. It values the portfolio at market prices, simulates price changes day by day and lets you buy and sell.

Each asset type has its own rules for transaction fees, capital gains tax, allowed order size and yearly income (dividends or coupons). When you buy, the average purchase price is recalculated. When you sell, fees and capital gains tax are taken off before the money goes back to the cash balance.

The portfolio and the market are loaded from two CSV files at startup, so no asset is written in the code.

## 2. How to run

### Requirements
- Java 25 or later
- Maven 3.9+

### Run the tests
```bash
mvn test
```

### Run the application in IntelliJ IDEA
1. Open the financial-portfolio-engine folder in IntelliJ. It is imported as a Maven project.
2. Open the Application class (package com.junia.portfolio) and click the green arrow next to main.
3. To choose the mode, go to Run > Edit Configurations... and select the Application configuration. Leave Program arguments empty for the console, or write --web for the web dashboard. The Working directory has to be the project root, because the data files are read from src/main/resources.
4. Click Apply and run it again.

In console mode the menu shows up in the Run tab at the bottom of IntelliJ, and you type your choices there. In web mode, open http://localhost:7070 in a browser. **The console is the main mode**; the web dashboard is an extra.

## 3. Features

- Loads the starting portfolio (assets.csv, which also holds the cash) and the market (market.csv)
- Shows each position with its quantity, average purchase price, market price, value, unrealized gain or loss, ROI, weight in the portfolio and price trend
- Shows the market, with the fee for buying one unit of each asset
- Buys more of an asset you already own (the average purchase price is updated) or a new asset from the market
- Sells with fees and capital gains tax taken off, and removes the position once everything is sold
- Checks every order: enough cash, a quantity allowed for that asset type, and enough units owned for a sale
- Estimates the yearly income of the portfolio from dividends and coupons
- Simulates a market day, where each price moves randomly between -4 % and +4 %
- Saves the portfolio after every order and on exit, and loads it back on the next start
- Resets the portfolio to its starting state

## 4. Main classes

Everything is in the com.junia.portfolio package.

| Package | Class | Role |
| --- | --- | --- |
| model | Asset | Abstract parent of every asset: symbol, quantity, average purchase price, and the shared rules (value, capital gain, adding and removing units) |
| model | AssetType | Enum of the asset types. It creates the right asset object from a line of the CSV file |
| model | Stock, CryptoAsset, Bond | The three asset types, each with its own fees, tax rate, order size and income |
| model | Taxable, Tradeable | Interfaces for capital gains tax and order size checks |
| model | Portfolio | Owned positions and cash balance |
| model | Order, OrderType, TradeReceipt | A buy or sell order, and what happened when it ran (fees, tax, net amount) |
| model | PositionSummary | Computed view of a position, used by the console and the web dashboard |
| service | PortfolioService | Runs orders and applies the transaction rules |
| service | MarketDataService | Listed assets, current prices, price history and simulation |
| service | AnalyticsService | Value, gains and losses, ROI, weights, yearly income, tax estimate |
| utils | AbstractCsvParser<T>, AssetCsvParser, MarketCsvParser | Read the CSV files and build the objects |
| repository | PortfolioRepository, CsvPortfolioRepository | Interface for saving and loading a portfolio, and its CSV version |
| utils | FinancialFormatter | Allocation bars and sparklines for the console |
| ui.console | ConsoleMenu | Talks to the user in the console |
| ui.web | PortfolioWebController | REST API behind the optional web dashboard |
| | Application | Entry point. Creates the objects, connects them and starts the console or the web dashboard |

## 5. Technical decisions

1. Orders use the market price. The user only picks the symbol and the quantity, and MarketDataService gives the price. If users could type a price, they could buy at 1 € and sell at the real price.

2. The market has its own file. Buying an asset you don't own yet needs its type and its parameters (dividend, network fee or coupon), so market.csv lists the assets you can buy. A purchase calls createPosition on the listed asset, which returns a new position of the right type. The service never needs a switch or an instanceof.

3. Asset types are an enum, AssetType. Each value knows how many extra parameters its asset needs, and its create method builds the right object. The parser calls AssetType.valueOf(type).create(...), so the switch on the type is written once, inside AssetType, and nowhere else. Assets only give out their data (getAssetType, getSpecificParameters), and CsvPortfolioRepository decides how the file looks.

4. Business and data errors are checked exceptions. Errors we expect to happen, like missing funds, an unknown asset, a bad order or a broken file, extend Exception, so the caller has to deal with them. Programming mistakes, like creating an asset with a negative quantity, throw an IllegalArgumentException.

5. Numbers are saved the same way whatever the system language is. On a French system, String.format("%.2f") writes 2500,00, and that comma would break a comma-separated file. Saving uses BigDecimal.toPlainString(), which always writes a dot.

## 6. Inheritance

Asset is the parent class of Stock, CryptoAsset and Bond. It holds what all assets share: symbol, quantity, average purchase price, market value, the taxable gain calculation, the average price update when buying and the removal of units when selling.

MarketCsvParser extends AssetCsvParser. Its file has different columns (a price instead of a quantity and a purchase price), but it reuses the code that creates the assets.

## 7. Polymorphism

The services only handle Asset objects, and what happens depends on the real type of each one:

| Method | Stock | CryptoAsset | Bond |
| --- | --- | --- | --- |
| calculateTransactionFee | 1 € + 0.1 % of the amount | network fee + 0.5 % | flat 5 € |
| calculateAnnualIncome | dividend | 0 | coupon on a 1000 € face value |
| getTaxRate | 30 % | 30 % | 15 % |
| isOrderSizeValid | whole number | down to 0.00000001 | whole number, at least 1 |
| createPosition | creates a Stock | creates a CryptoAsset | creates a Bond |
| getSpecificParameters | dividend rate | network fee | coupon rate, maturity year |

For example, AnalyticsService adds up the yearly income without checking any type:

```java
portfolio.getAssets().stream()
        .mapToDouble(asset -> asset.calculateAnnualIncome(marketDataService.getCurrentPrice(asset)))
        .sum();
```

And PortfolioService gets the fee for a purchase the same way:

```java
double fee = reference.calculateTransactionFee(order.getQuantity(), order.getPrice());
```

## 8. Abstract class and interfaces

Asset is abstract because a plain "asset" with no type makes no sense, so it should never be created. It holds state and shared code (average price, capital gain), which an interface can't do, and it makes every subclass write the methods that depend on its type.

There are three interfaces:

- Taxable is for anything taxed on a capital gain (calculateTaxableGain, getTaxRate).
- Tradeable holds the order size rule (isOrderSizeValid).
- PortfolioRepository lists what the application needs to load and save a portfolio, without knowing it is stored in a CSV file.

Taxable and Tradeable are kept small and apart, one ability each. Asset implements both, since here every asset can be traded and is taxed on its gains. The capital gain calculation is the same for everyone and lives in Asset, while each subclass sets its own tax rate and order size rule.

## 9. Custom exceptions

They are in the com.junia.portfolio.exception package and all extend Exception.

- InsufficientFundsException: a purchase, fees included, costs more than the cash available.
- AssetNotFoundException: selling an asset you don't own, or buying a symbol that isn't on the market.
- InvalidTransactionException: a quantity or price that is zero or negative, a quantity this asset type doesn't allow (2.5 shares, for example), selling more units than you own, or a sale too small to pay the fees.
- InvalidDataFileException: a bad line in a file (wrong number of columns, badly written number, unknown asset type, wrong number of parameters, negative values, duplicate symbol). The message gives the file name and the line.

The application also uses three standard exceptions: IOException when a file is missing or can't be read, NumberFormatException for bad user input, and IllegalArgumentException for invalid data when an object is built.

## 10. Data loading

Two files are read at startup from src/main/resources:

- market.csv holds the assets on the market and their starting price, with the columns TYPE, SYMBOL, PRICE, EXTRA_PARAM, EXTRA_PARAM2. MarketCsvParser reads it.
- assets.csv holds the starting portfolio, with a CASH line for the cash balance. Its columns are TYPE, SYMBOL, QUANTITY, PURCHASE_PRICE, EXTRA_PARAM, EXTRA_PARAM2. CsvPortfolioRepository reads it with AssetCsvParser.

AbstractCsvParser<T> reads the file with Files.lines(), skips the header with .skip(1), ignores blank lines and passes each line to the abstract parseLine method (the Template Method pattern). The TYPE column becomes an AssetType, which creates a Stock, a CryptoAsset or a Bond.

If a saved state exists in data/portfolio_state.csv (same format as assets.csv), it is loaded instead of the starting portfolio. If that file is broken, the application says so and starts from assets.csv. Market prices aren't saved and start again from market.csv every time.

## 11. SOLID principles

- Single Responsibility: ConsoleMenu only talks to the user, PortfolioService handles orders, AnalyticsService the indicators, MarketDataService the prices, CsvPortfolioRepository the saving and the parsers the file reading. The model classes don't know the file format, so changing it only touches the repository. Application only creates and connects the objects.
- Open/Closed: to add a new asset type, say RealEstateAsset, you write a subclass of Asset and add a value and a case to AssetType. The parser, the repository, PortfolioService and AnalyticsService stay the same, because none of them has an if or a switch on the type, and fees, income, tax and order size are methods of the asset.
- Liskov Substitution: any subclass of Asset works wherever an Asset is expected. The tests call the same methods on a List<Asset> holding all three types.
- Interface Segregation: Taxable and Tradeable are two short interfaces, each with methods that belong together.
- Dependency Inversion: objects get what they need through their constructor. ConsoleMenu and PortfolioWebController use the PortfolioRepository interface, and only Application chooses CsvPortfolioRepository, so the storage could move to a database without changing the user interface. MarketDataService is given its Random, which lets the tests get the same simulation every run.

## 12. Tests

The JUnit 5 tests are in src/test/java/com/junia/portfolio and run with mvn test. They check normal cases, edge cases and errors:

- model.AssetTest: fees, income and tax for each asset type (with a polymorphic test on a List<Asset>), order size, average purchase price, creating positions, creating assets through AssetType, data checks.
- model.PortfolioTest: finding an asset, duplicate symbols, changing the returned list doesn't change the portfolio, negative cash.
- service.PortfolioServiceTest: buying an asset you own or a new one, fees, not enough cash (also when the fees make the difference), spending exactly the whole balance, bad quantities, selling with and without tax, selling everything, sales that can't happen.
- service.AnalyticsServiceTest: total value, gains and losses, ROI, yearly income, tax estimate, weights, position summary.
- service.MarketDataServiceTest: starting prices, history, invalid price, daily change staying within its limits.
- utils.AssetCsvParserTest and utils.MarketCsvParserTest: reading a good file, blank lines, bad numbers, unknown type, missing columns, missing file, duplicate symbol in the market file.
- repository.CsvPortfolioRepositoryTest: saving and reloading every field, line format, French locale, reset, and a broken file that doesn't wipe the portfolio in memory.
- utils.FinancialFormatterTest: bars and sparklines, including negative values.
- EndToEndPortfolioFlowTest: a full run on the real files (buying a new asset, simulating, selling everything, saving and loading back).
