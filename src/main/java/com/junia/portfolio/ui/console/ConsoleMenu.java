package com.junia.portfolio.ui.console;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.PositionSummary;
import com.junia.portfolio.model.TradeReceipt;
import com.junia.portfolio.repository.PortfolioRepository;
import com.junia.portfolio.service.AnalyticsService;
import com.junia.portfolio.service.MarketDataService;
import com.junia.portfolio.service.PortfolioService;
import com.junia.portfolio.utils.FinancialFormatter;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleMenu {
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final AnalyticsService analyticsService;
    private final PortfolioRepository repository;
    private final Scanner scanner;

    public ConsoleMenu(PortfolioService portfolioService, MarketDataService marketDataService,
                       AnalyticsService analyticsService, PortfolioRepository repository, Scanner scanner) {
        this.portfolioService = portfolioService;
        this.marketDataService = marketDataService;
        this.analyticsService = analyticsService;
        this.repository = repository;
        this.scanner = scanner;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                break;
            }

            switch (readInput()) {
                case "1":
                    displayPortfolio();
                    break;
                case "2":
                    displayMarket();
                    break;
                case "3":
                    placeOrder(OrderType.BUY);
                    break;
                case "4":
                    placeOrder(OrderType.SELL);
                    break;
                case "5":
                    simulateMarketDay();
                    break;
                case "6":
                    resetPortfolio();
                    break;
                case "7":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
        savePortfolio();
        System.out.println("Portfolio saved. Goodbye.");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. View portfolio");
        System.out.println("2. View market");
        System.out.println("3. Buy asset");
        System.out.println("4. Sell asset");
        System.out.println("5. Simulate market day");
        System.out.println("6. Reset portfolio to initial state");
        System.out.println("7. Save and exit");
        System.out.print("> ");
    }

    private void displayPortfolio() {
        Portfolio portfolio = portfolioService.getPortfolio();

        System.out.println();
        System.out.printf("%-8s %-7s %-12s %-12s %-12s %-12s %-11s %-9s %-13s %s%n",
                "Symbol", "Type", "Quantity", "Purchase", "Market", "Value", "P&L", "ROI", "Allocation", "Trend");

        for (PositionSummary position : analyticsService.summarizePositions(portfolio)) {
            System.out.printf("%-8s %-7s %-12.4f %-12.2f %-12.2f %-12.2f %+-11.2f %-9s %-13s %s%n",
                    position.getSymbol(),
                    position.getAssetType(),
                    position.getQuantity(),
                    position.getPurchasePrice(),
                    position.getMarketPrice(),
                    position.getCurrentValue(),
                    position.getGainLoss(),
                    String.format("%+.2f%%", position.getReturnOnInvestment()),
                    FinancialFormatter.formatBarChart(position.getAllocation(), 10),
                    FinancialFormatter.formatSparkline(marketDataService.getPriceHistory(position.getSymbol())));
        }

        System.out.println();
        System.out.printf("Cash balance:            %.2f%n", portfolio.getCashBalance());
        System.out.printf("Invested (cost basis):   %.2f%n", analyticsService.calculateTotalCostBasis(portfolio));
        System.out.printf("Unrealized P&L:          %+.2f%n", analyticsService.calculateTotalUnrealizedGainLoss(portfolio));
        System.out.printf("Projected annual income: %.2f%n", analyticsService.calculateProjectedAnnualIncome(portfolio));
        System.out.printf("Total portfolio value:   %.2f%n", analyticsService.calculateTotalValue(portfolio));
    }

    private void displayMarket() {
        System.out.println();
        System.out.printf("%-8s %-7s %-12s %-24s %s%n", "Symbol", "Type", "Price", "Fee for 1 unit", "Trend");
        for (Asset listing : marketDataService.getListings()) {
            double price = marketDataService.getCurrentPrice(listing);
            System.out.printf("%-8s %-7s %-12.2f %-24.2f %s%n",
                    listing.getSymbol(),
                    listing.getAssetType(),
                    price,
                    listing.calculateTransactionFee(1, price),
                    FinancialFormatter.formatSparkline(marketDataService.getPriceHistory(listing.getSymbol())));
        }
    }

    private void placeOrder(OrderType type) {
        System.out.print("Symbol: ");
        String symbol = readInput().toUpperCase();
        System.out.print("Quantity: ");
        String quantityInput = readInput();

        try {
            double quantity = Double.parseDouble(quantityInput);
            if (type == OrderType.SELL && !confirmSale(symbol, quantity)) {
                System.out.println("Sale cancelled.");
                return;
            }

            TradeReceipt receipt = portfolioService.placeMarketOrder(type, symbol, quantity);
            printReceipt(receipt);
            savePortfolio();
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity: " + quantityInput);
        } catch (InsufficientFundsException | AssetNotFoundException | InvalidTransactionException e) {
            System.out.println("Order refused: " + e.getMessage());
        }
    }

    private boolean confirmSale(String symbol, double quantity) {
        Optional<Asset> asset = portfolioService.findAssetBySymbol(symbol);
        if (asset.isEmpty() || !asset.get().isOrderSizeValid(quantity) || !asset.get().holdsAtLeast(quantity)) {
            return true;
        }

        System.out.printf("Estimated capital gain tax: %.2f. Confirm sale? (y/n): ",
                analyticsService.estimateSellingTax(asset.get(), quantity));
        return readInput().equalsIgnoreCase("y");
    }

    private String readInput() {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    private void printReceipt(TradeReceipt receipt) {
        String action = receipt.getOrder().getType() == OrderType.BUY ? "Bought" : "Sold";
        String total = receipt.getOrder().getType() == OrderType.BUY ? "Total paid" : "Net received";
        System.out.printf("%s %s %s at %.2f | Fees: %.2f | Tax: %.2f | %s: %.2f%n",
                action,
                FinancialFormatter.formatQuantity(receipt.getOrder().getQuantity()),
                receipt.getOrder().getSymbol(),
                receipt.getOrder().getPrice(),
                receipt.getFee(),
                receipt.getTax(),
                total,
                receipt.getNetAmount());
    }

    private void simulateMarketDay() {
        double valueBefore = analyticsService.calculateTotalValue(portfolioService.getPortfolio());
        marketDataService.simulateMarketDay();
        double valueAfter = analyticsService.calculateTotalValue(portfolioService.getPortfolio());
        System.out.printf("Simulated 1 market day. Portfolio value: %.2f -> %.2f (%+.2f)%n",
                valueBefore, valueAfter, valueAfter - valueBefore);
    }

    private void resetPortfolio() {
        try {
            repository.resetToInitial(portfolioService.getPortfolio());
            System.out.println("Portfolio reset to initial state.");
        } catch (IOException | InvalidDataFileException e) {
            System.out.println("Could not reset portfolio: " + e.getMessage());
        }
    }

    private void savePortfolio() {
        try {
            repository.save(portfolioService.getPortfolio());
        } catch (IOException e) {
            System.out.println("Could not save portfolio state: " + e.getMessage());
        }
    }
}
