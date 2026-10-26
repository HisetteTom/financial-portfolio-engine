package com.junia.portfolio;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Order;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.service.AnalyticsService;
import com.junia.portfolio.service.MarketDataService;
import com.junia.portfolio.service.PortfolioService;
import com.junia.portfolio.utils.AssetCsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndToEndPortfolioFlowTest {
    private Portfolio portfolio;
    private PortfolioService portfolioService;
    private MarketDataService marketDataService;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(10000.0);
        portfolioService = new PortfolioService(portfolio, new AssetCsvParser());
        portfolioService.loadAssetsFromCsv(Paths.get("src/main/resources/assets.csv"));

        Map<String, Double> prices = new HashMap<>();
        prices.put("AAPL", 180.0);
        prices.put("MSFT", 330.0);
        prices.put("BTC", 62000.0);
        prices.put("ETH", 3200.0);
        prices.put("US10Y", 1000.0);
        prices.put("FRTR", 980.0);
        marketDataService = new MarketDataService(prices);
        analyticsService = new AnalyticsService();
    }

    @Test
    void shouldExecuteFullTradingAndSimulationLifecycle() throws Exception {
        assertEquals(6, portfolio.getAssets().size());
        assertEquals(10000.0, portfolio.getCashBalance(), 0.001);

        Order buyAapl = new Order("AAPL", OrderType.BUY, 5.0, 180.0);
        portfolioService.executeOrder(buyAapl);
        assertEquals(9100.0, portfolio.getCashBalance(), 0.001);

        Asset aapl = portfolioService.findAssetBySymbol("AAPL").orElseThrow();
        assertEquals(20.0, aapl.getQuantity(), 0.001);

        marketDataService.simulateMarketDay();
        double currentAaplPrice = marketDataService.getPrice("AAPL");
        assertTrue(currentAaplPrice > 0.0);

        double unrealizedPnl = analyticsService.calculateUnrealizedGainLoss(aapl, currentAaplPrice);
        assertTrue(unrealizedPnl != 0.0 || currentAaplPrice == aapl.getPurchasePrice());

        Order sellAapl = new Order("AAPL", OrderType.SELL, 10.0, currentAaplPrice);
        portfolioService.executeOrder(sellAapl);

        assertEquals(10.0, aapl.getQuantity(), 0.001);
        assertEquals(9100.0 + (10.0 * currentAaplPrice), portfolio.getCashBalance(), 0.001);
    }
}
