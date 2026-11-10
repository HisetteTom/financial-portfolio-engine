package com.junia.portfolio.ui.web;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.TradeReceipt;
import com.junia.portfolio.repository.PortfolioRepository;
import com.junia.portfolio.service.AnalyticsService;
import com.junia.portfolio.service.MarketDataService;
import com.junia.portfolio.service.PortfolioService;
import com.junia.portfolio.utils.FinancialFormatter;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PortfolioWebController {
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final AnalyticsService analyticsService;
    private final PortfolioRepository repository;
    private final List<Double> portfolioHistory;

    public PortfolioWebController(PortfolioService portfolioService, MarketDataService marketDataService,
                                  AnalyticsService analyticsService, PortfolioRepository repository) {
        this.portfolioService = portfolioService;
        this.marketDataService = marketDataService;
        this.analyticsService = analyticsService;
        this.repository = repository;
        this.portfolioHistory = new ArrayList<>();
        this.portfolioHistory.add(currentTotalValue());
    }

    public void start(int port) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.routes.get("/api/portfolio", this::getPortfolio);
            config.routes.post("/api/market/simulate", this::simulateMarketDay);
            config.routes.post("/api/portfolio/reset", this::resetPortfolio);
            config.routes.post("/api/orders", this::placeOrder);
        });
        app.start(port);
    }

    private void getPortfolio(Context ctx) {
        Portfolio portfolio = portfolioService.getPortfolio();

        Map<String, Object> response = new HashMap<>();
        response.put("cashBalance", portfolio.getCashBalance());
        response.put("totalPortfolioValue", analyticsService.calculateTotalValue(portfolio));
        response.put("totalCostBasis", analyticsService.calculateTotalCostBasis(portfolio));
        response.put("totalUnrealizedPnl", analyticsService.calculateTotalUnrealizedGainLoss(portfolio));
        response.put("projectedAnnualIncome", analyticsService.calculateProjectedAnnualIncome(portfolio));
        response.put("portfolioHistory", List.copyOf(portfolioHistory));
        response.put("allMarketPrices", marketDataService.getAllPrices());
        response.put("assets", analyticsService.summarizePositions(portfolio));
        ctx.json(response);
    }

    private void simulateMarketDay(Context ctx) {
        marketDataService.simulateMarketDay();
        portfolioHistory.add(currentTotalValue());
        ctx.result("Market day simulated");
    }

    private void resetPortfolio(Context ctx) {
        try {
            repository.resetToInitial(portfolioService.getPortfolio());
            portfolioHistory.clear();
            portfolioHistory.add(currentTotalValue());
            ctx.result("Portfolio reset to initial state");
        } catch (IOException | InvalidDataFileException e) {
            ctx.status(500).result("Reset failed: " + e.getMessage());
        }
    }

    private void placeOrder(Context ctx) {
        OrderRequest request = ctx.bodyAsClass(OrderRequest.class);
        if (request.type == null || request.symbol == null) {
            ctx.status(400).result("Symbol and order type are required.");
            return;
        }

        OrderType type;
        try {
            type = OrderType.valueOf(request.type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.status(400).result("Invalid order type: " + request.type);
            return;
        }

        try {
            TradeReceipt receipt = portfolioService.placeMarketOrder(type, request.symbol, request.quantity);
            portfolioHistory.set(portfolioHistory.size() - 1, currentTotalValue());
            ctx.status(201).result(describe(receipt) + saveState());
        } catch (InsufficientFundsException | AssetNotFoundException | InvalidTransactionException e) {
            ctx.status(400).result(e.getMessage());
        }
    }

    private String saveState() {
        try {
            repository.save(portfolioService.getPortfolio());
            return "";
        } catch (IOException e) {
            return " (state could not be saved: " + e.getMessage() + ")";
        }
    }

    private String describe(TradeReceipt receipt) {
        String action = receipt.getOrder().getType() == OrderType.BUY ? "Bought" : "Sold";
        return String.format(Locale.ROOT, "%s %s %s at %.2f € | fees %.2f €, tax %.2f €, total %.2f €",
                action,
                FinancialFormatter.formatQuantity(receipt.getOrder().getQuantity()),
                receipt.getOrder().getSymbol(),
                receipt.getOrder().getPrice(),
                receipt.getFee(),
                receipt.getTax(),
                receipt.getNetAmount());
    }

    private double currentTotalValue() {
        return analyticsService.calculateTotalValue(portfolioService.getPortfolio());
    }

    public static class OrderRequest {
        public String symbol;
        public String type;
        public double quantity;
    }
}
