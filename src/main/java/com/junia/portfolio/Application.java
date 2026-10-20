package com.junia.portfolio;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Order;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.service.PortfolioService;
import com.junia.portfolio.utils.AssetCsvParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Application {
    public static void main(String[] args) {
        Portfolio portfolio = new Portfolio(5000.0);
        AssetCsvParser parser = new AssetCsvParser();
        PortfolioService service = new PortfolioService(portfolio, parser);

        Path csvPath = Paths.get("src/main/resources/assets.csv");
        service.loadAssetsFromCsv(csvPath);

        Map<String, Double> marketPrices = new HashMap<>();
        marketPrices.put("AAPL", 185.0);
        marketPrices.put("MSFT", 340.0);
        marketPrices.put("BTC", 64000.0);
        marketPrices.put("ETH", 3250.0);
        marketPrices.put("US10Y", 1010.0);
        marketPrices.put("FRTR", 990.0);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("1. View portfolio");
            System.out.println("2. Buy asset");
            System.out.println("3. Sell asset");
            System.out.println("4. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> displayPortfolio(service, marketPrices);
                case "2" -> handleBuy(service, scanner);
                case "3" -> handleSell(service, scanner);
                case "4" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void displayPortfolio(PortfolioService service, Map<String, Double> marketPrices) {
        Portfolio portfolio = service.getPortfolio();
        System.out.println();
        System.out.printf("%-10s %-12s %-16s %-16s %-16s%n", "Symbol", "Quantity", "Purchase Price", "Market Price", "Current Value");

        for (Asset asset : portfolio.getAssets()) {
            double marketPrice = marketPrices.getOrDefault(asset.getSymbol(), asset.getPurchasePrice());
            double currentValue = asset.calculateCurrentValue(marketPrice);
            System.out.printf("%-10s %-12.4f %-16.2f %-16.2f %-16.2f%n",
                    asset.getSymbol(),
                    asset.getQuantity(),
                    asset.getPurchasePrice(),
                    marketPrice,
                    currentValue);
        }

        System.out.println();
        System.out.printf("Cash balance: %.2f%n", portfolio.getCashBalance());
        System.out.printf("Total portfolio value: %.2f%n", service.calculateTotalValue(marketPrices));
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

    private static void handleSell(PortfolioService service, Scanner scanner) {
        try {
            System.out.print("Symbol: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            System.out.print("Quantity: ");
            double quantity = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Price: ");
            double price = Double.parseDouble(scanner.nextLine().trim());

            Order order = new Order(symbol, OrderType.SELL, quantity, price);
            service.executeOrder(order);
            System.out.println("Order executed successfully.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (InsufficientFundsException | AssetNotFoundException | InvalidTransactionException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
