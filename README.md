# Financial Portfolio Engine

This is my Java OOP final project. It's a small app to manage an investment portfolio with stocks, crypto and bonds. You can see what your portfolio is worth, buy and sell, and simulate market days to watch the prices move.

Buying and selling isn't free: each asset type has its own fees and capital gains tax, and you can only order sizes that make sense for it (no 2.5 shares, for example). The starting portfolio and the market come from two CSV files.

## How to run it

You need Java 25 and Maven.

In IntelliJ, open the financial-portfolio-engine folder (it's a Maven project), open Application and click the green arrow next to main. The menu shows up in the Run tab and you type your choices there.

There's also a small web dashboard. To get it, go to Run > Edit Configurations, put --web in Program arguments and run again, then open http://localhost:7070. Leave the field empty to get the console back, which is the main mode. In both cases the working directory has to be the project root, since the data files are read from src/main/resources.

To run the tests:

```bash
mvn test
```

## Features

- Load the starting portfolio and cash from assets.csv, and the market from market.csv
- Show each position with its value, gain or loss, ROI, weight and a small price trend
- Buy an asset you own or a new one from the market (the average purchase price is updated)
- Sell with fees and tax taken off, and refuse orders that aren't possible (not enough cash, bad quantity, selling more than you own)
- Estimate the yearly income from dividends and coupons
- Simulate a market day, where each price moves between -4 % and +4 %
- Save after every order and on exit, reload on the next start, or reset to the starting state

## Main classes

- model: Asset (abstract) and its subclasses Stock, CryptoAsset and Bond, the AssetType enum, Portfolio, Order and TradeReceipt
- service: PortfolioService runs the orders, MarketDataService holds the prices and the simulation, AnalyticsService does the calculations (value, ROI, income, tax)
- utils: AbstractCsvParser, AssetCsvParser and MarketCsvParser read the files, FinancialFormatter draws the bars and trends in the console
- repository: PortfolioRepository and CsvPortfolioRepository save and load the portfolio
- ui: ConsoleMenu for the console, PortfolioWebController for the web dashboard
- Application creates the objects, connects them and starts the right mode

## Technical decisions

1. Orders always use the market price. The user only picks the symbol and the quantity. If they could type a price, they could buy at 1 € and sell at the real price.
2. AssetType is an enum with a create method. It's the only place with a switch on the type, so the parser and the services work with any Asset without knowing which one it is.
3. Numbers are saved with BigDecimal. On a French system, String.format writes 2500,00, and that comma would break the CSV file. BigDecimal always writes a dot.

## Inheritance, polymorphism, abstract class and interfaces

Asset is the abstract parent of Stock, CryptoAsset and Bond. It holds what they share (symbol, quantity, average price, gain calculation) and leaves the type-specific methods abstract. It's abstract because an asset with no type makes no sense. MarketCsvParser also extends AssetCsvParser to reuse the code that builds assets.

The services only work with Asset objects, and each subclass answers in its own way:

| Method | Stock | CryptoAsset | Bond |
| --- | --- | --- | --- |
| calculateTransactionFee | 1 € + 0.1 % | network fee + 0.5 % | 5 € |
| calculateAnnualIncome | dividend | 0 | coupon |
| getTaxRate | 30 % | 30 % | 15 % |
| isOrderSizeValid | whole number | down to 0.00000001 | whole number |

There are three interfaces. Taxable (tax rate and taxable gain) and Tradeable (order size rule) are small and separate, and Asset implements both. PortfolioRepository describes saving and loading, so the menu doesn't know the data is in a CSV file.

## Custom exceptions

All four extend Exception, so the caller has to handle them:

- InsufficientFundsException when a purchase plus fees costs more than the cash
- AssetNotFoundException for an unknown symbol or an asset you don't own
- InvalidTransactionException for a bad quantity, selling too much, or a sale that doesn't cover the fees
- InvalidDataFileException for a broken line in a CSV file (the message gives the file and the line)

## Data loading

At startup the app reads market.csv and assets.csv from src/main/resources. AbstractCsvParser reads the lines with Files.lines, skips the header and blank lines, and calls parseLine for each one. The TYPE column goes through AssetType to build the right object. If a saved state exists in data/portfolio_state.csv it's loaded instead, and if that file is broken the app says so and starts from assets.csv.

## SOLID

- Single responsibility: the menu only talks to the user, the services do the logic, the parsers read files and the repository saves.
- Open/closed: a new asset type means a new subclass and one case in AssetType, nothing else changes.
- Liskov: every subclass works wherever an Asset is expected, and the tests check it on a List<Asset>.
- Interface segregation: Taxable and Tradeable are two small interfaces instead of one big one.
- Dependency inversion: classes get what they need through their constructor, and the menu uses PortfolioRepository, not the CSV class. MarketDataService also gets its Random from outside, so tests can use a fixed seed.

## Tests

There are 83 JUnit 5 tests in src/test/java, run with mvn test. They cover the normal cases, edge cases like spending exactly all the cash or selling everything, and errors like missing funds, bad quantities, broken files or duplicate symbols. One test also runs the whole flow on the real files: buy, simulate, sell, save and reload.
