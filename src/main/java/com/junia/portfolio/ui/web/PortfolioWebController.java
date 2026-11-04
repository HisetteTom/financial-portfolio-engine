package com.junia.portfolio.ui.web;

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
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioWebController {
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final AnalyticsService analyticsService;
    private final List<Double> portfolioHistory;

    public PortfolioWebController(PortfolioService portfolioService,
                                  MarketDataService marketDataService,
                                  AnalyticsService analyticsService) {
        this.portfolioService = portfolioService;
        this.marketDataService = marketDataService;
        this.analyticsService = analyticsService;
        this.portfolioHistory = new ArrayList<>();

        double initialTotal = portfolioService.calculateTotalValue(marketDataService.getAllPrices());
        this.portfolioHistory.add(initialTotal);
    }

    public void start(int port) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);

            config.routes.get("/api/portfolio", ctx -> {
                Portfolio portfolio = portfolioService.getPortfolio();
                Map<String, Double> prices = marketDataService.getAllPrices();
                double totalValue = portfolioService.calculateTotalValue(prices);

                double totalCostBasis = portfolio.getAssets().stream()
                        .mapToDouble(a -> a.getPurchasePrice() * a.getQuantity())
                        .sum();
                double totalUnrealizedPnl = portfolio.getAssets().stream()
                        .mapToDouble(a -> {
                            double p = marketDataService.getPrice(a.getSymbol());
                            return (p == 0.0 ? a.getPurchasePrice() : p) * a.getQuantity() - (a.getPurchasePrice() * a.getQuantity());
                        })
                        .sum();

                List<Map<String, Object>> assetDtos = new ArrayList<>();
                for (Asset asset : portfolio.getAssets()) {
                    double marketPrice = marketDataService.getPrice(asset.getSymbol());
                    if (marketPrice == 0.0) {
                        marketPrice = asset.getPurchasePrice();
                    }
                    double currentValue = asset.calculateCurrentValue(marketPrice);
                    double gainLoss = analyticsService.calculateUnrealizedGainLoss(asset, marketPrice);
                    double allocation = analyticsService.calculateAssetAllocationPercentage(portfolio, asset, marketPrice, totalValue);
                    List<Double> history = marketDataService.getPriceHistory(asset.getSymbol());

                    Map<String, Object> dto = new HashMap<>();
                    dto.put("symbol", asset.getSymbol());
                    dto.put("quantity", asset.getQuantity());
                    dto.put("purchasePrice", asset.getPurchasePrice());
                    dto.put("marketPrice", marketPrice);
                    dto.put("currentValue", currentValue);
                    dto.put("gainLoss", gainLoss);
                    dto.put("allocation", allocation);
                    dto.put("history", history);
                    assetDtos.add(dto);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("cashBalance", portfolio.getCashBalance());
                response.put("totalPortfolioValue", totalValue);
                response.put("totalCostBasis", totalCostBasis);
                response.put("totalUnrealizedPnl", totalUnrealizedPnl);
                response.put("portfolioHistory", Collections.unmodifiableList(portfolioHistory));
                response.put("allMarketPrices", prices);
                response.put("assets", assetDtos);

                ctx.json(response);
            });

            config.routes.post("/api/market/simulate", ctx -> {
                marketDataService.simulateMarketDay();
                double newTotal = portfolioService.calculateTotalValue(marketDataService.getAllPrices());
                portfolioHistory.add(newTotal);
                ctx.status(200).result("Market day simulated");
            });

            config.routes.post("/api/orders", ctx -> {
                OrderDto request = ctx.bodyAsClass(OrderDto.class);
                try {
                    OrderType type = OrderType.valueOf(request.type.toUpperCase());
                    Order order = new Order(request.symbol, type, request.quantity, request.price);
                    portfolioService.executeOrder(order);

                    double newTotal = portfolioService.calculateTotalValue(marketDataService.getAllPrices());
                    if (!portfolioHistory.isEmpty()) {
                        portfolioHistory.set(portfolioHistory.size() - 1, newTotal);
                    } else {
                        portfolioHistory.add(newTotal);
                    }

                    ctx.status(201).result("Order executed successfully");
                } catch (IllegalArgumentException e) {
                    ctx.status(400).result("Invalid order type: " + request.type);
                } catch (InsufficientFundsException | AssetNotFoundException | InvalidTransactionException e) {
                    ctx.status(400).result(e.getMessage());
                }
            });
        });

        app.start(port);
    }

    public static class OrderDto {
        public String symbol;
        public String type;
        public double quantity;
        public double price;
    }
}
