package com.junia.portfolio;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Order;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.service.AnalyticsService;
import com.junia.portfolio.service.MarketDataService;
import com.junia.portfolio.service.PortfolioService;
import com.junia.portfolio.ui.web.PortfolioWebController;
import com.junia.portfolio.utils.AssetCsvParser;
import com.junia.portfolio.utils.FinancialFormatter;
import com.junia.portfolio.utils.PortfolioStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Application {
    public static void main(String[] args) {
        Portfolio portfolio = new Portfolio(5000.0);
        AssetCsvParser parser = new AssetCsvParser();
        PortfolioService portfolioService = new PortfolioService(portfolio, parser);
        AnalyticsService analyticsService = new AnalyticsService();

        Path statePath = Paths.get("data/portfolio_state.csv");
        if (Files.exists(statePath)) {
            try {
                PortfolioStorage.load(portfolio, statePath, parser);
            } catch (IOException e) {
                System.out.println("Could not load saved state, falling back to initial csv.");
                portfolioService.loadAssetsFromCsv(Paths.get("src/main/resources/assets.csv"));
            }
        } else {
            portfolioService.loadAssetsFromCsv(Paths.get("src/main/resources/assets.csv"));
        }

        Map<String, Double> initialPrices = new HashMap<>();
        initialPrices.put("AAPL", 185.0);
        initialPrices.put("MSFT", 340.0);
        initialPrices.put("BTC", 64000.0);
        initialPrices.put("ETH", 3250.0);
        initialPrices.put("US10Y", 1010.0);
        initialPrices.put("FRTR", 990.0);

        MarketDataService marketDataService = new MarketDataService(initialPrices);

        if (args.length > 0 && args[0].equalsIgnoreCase("--web")) {
            int port = 7070;
            PortfolioWebController webController = new PortfolioWebController(portfolioService, marketDataService, analyticsService, statePath);
            webController.start(port);
            System.out.println("Web dashboard running at http://localhost:" + port);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("1. View portfolio");
            System.out.println("2. Buy asset");
            System.out.println("3. Sell asset");
            System.out.println("4. Simulate market day");
            System.out.println("5. Reset portfolio to initial state");
            System.out.println("6. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> displayPortfolio(portfolioService, marketDataService, analyticsService);
                case "2" -> handleBuy(portfolioService, scanner);
                case "3" -> handleSell(portfolioService, marketDataService, analyticsService, scanner);
                case "4" -> simulateMarket(marketDataService);
                case "5" -> resetPortfolio(portfolioService, statePath);
                case "6" -> {
                    savePortfolioState(portfolio, statePath);
                    running = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void displayPortfolio(PortfolioService portfolioService, MarketDataService marketDataService, AnalyticsService analyticsService) {
        Portfolio portfolio = portfolioService.getPortfolio();
        Map<String, Double> currentPrices = marketDataService.getAllPrices();
        double totalPortfolioValue = portfolioService.calculateTotalValue(currentPrices);

        System.out.println();
        System.out.printf("%-8s %-10s %-12s %-12s %-12s %-10s %-14s %-8s%n",
                "Symbol", "Quantity", "Purchase", "Market", "Value", "P&L (€)", "Allocation", "Trend");

        for (Asset asset : portfolio.getAssets()) {
            double marketPrice = marketDataService.getPrice(asset.getSymbol());
            if (marketPrice == 0.0) {
                marketPrice = asset.getPurchasePrice();
            }

            double currentValue = asset.calculateCurrentValue(marketPrice);
            double gainLoss = analyticsService.calculateUnrealizedGainLoss(asset, marketPrice);
            double allocation = analyticsService.calculateAssetAllocationPercentage(portfolio, asset, marketPrice, totalPortfolioValue);
            String bar = FinancialFormatter.formatBarChart(allocation, 10);
            List<Double> history = marketDataService.getPriceHistory(asset.getSymbol());
            String sparkline = FinancialFormatter.formatSparkline(history);

            System.out.printf("%-8s %-10.4f %-12.2f %-12.2f %-12.2f %-+10.2f %-14s %-8s%n",
                    asset.getSymbol(),
                    asset.getQuantity(),
                    asset.getPurchasePrice(),
                    marketPrice,
                    currentValue,
                    gainLoss,
                    bar,
                    sparkline);
        }

        System.out.println();
        System.out.printf("Cash balance: %.2f%n", portfolio.getCashBalance());
        System.out.printf("Total portfolio value: %.2f%n", totalPortfolioValue);
    }

    private static void handleBuy(PortfolioService service, Scanner scanner) {
        try {
            System.out.print("Symbol: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            System.out.print("Quantity: ");
            double quantity = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Price: ");
            double price = Double.parseDouble(scanner.nextLine().trim());

            Order order = new Order(symbol, OrderType.BUY, quantity, price);
            service.executeOrder(order);
            System.out.println("Order executed successfully.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (InsufficientFundsException | AssetNotFoundException | InvalidTransactionException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void handleSell(PortfolioService service, MarketDataService marketDataService, AnalyticsService analyticsService, Scanner scanner) {
        try {
            System.out.print("Symbol: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            System.out.print("Quantity: ");
            double quantity = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Price: ");
            double price = Double.parseDouble(scanner.nextLine().trim());

            service.findAssetBySymbol(symbol).ifPresent(asset -> {
                double estimatedTax = analyticsService.estimateSellingTax(asset, price, quantity);
                System.out.printf("Estimated capital gain tax: %.2f%n", estimatedTax);
            });

            Order order = new Order(symbol, OrderType.SELL, quantity, price);
            service.executeOrder(order);
            System.out.println("Order executed successfully.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (InsufficientFundsException | AssetNotFoundException | InvalidTransactionException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void resetPortfolio(PortfolioService service, Path statePath) {
        service.getPortfolio().getAssets().clear();
        service.getPortfolio().setCashBalance(5000.0);
        service.loadAssetsFromCsv(Paths.get("src/main/resources/assets.csv"));
        try {
            Files.deleteIfExists(statePath);
            System.out.println("Portfolio reset to initial state.");
        } catch (IOException e) {
            System.out.println("Error deleting saved state: " + e.getMessage());
        }
    }

    private static void simulateMarket(MarketDataService marketDataService) {
        marketDataService.simulateMarketDay();
        System.out.println("Simulated 1 market day. Prices updated.");
    }

    private static void savePortfolioState(Portfolio portfolio, Path path) {
        try {
            PortfolioStorage.save(portfolio, path);
            System.out.println("Portfolio state saved to " + path);
        } catch (IOException e) {
            System.out.println("Failed to save state: " + e.getMessage());
        }
    }
}
