package com.junia.portfolio;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Stock;
import com.junia.portfolio.model.TradeReceipt;
import com.junia.portfolio.repository.CsvPortfolioRepository;
import com.junia.portfolio.repository.PortfolioRepository;
import com.junia.portfolio.service.AnalyticsService;
import com.junia.portfolio.service.MarketDataService;
import com.junia.portfolio.service.PortfolioService;
import com.junia.portfolio.utils.AssetCsvParser;
import com.junia.portfolio.utils.MarketCsvParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndToEndPortfolioFlowTest {

    @Test
    void shouldLoadTradeSimulateAndRestorePortfolio(@TempDir Path tempDir) throws Exception {
        MarketDataService market = new MarketDataService(
                new MarketCsvParser().parse(Path.of("src/main/resources/market.csv")), new Random(7));
        PortfolioRepository repository = new CsvPortfolioRepository(
                Path.of("src/main/resources/assets.csv"), tempDir.resolve("state.csv"), new AssetCsvParser());
        Portfolio portfolio = new Portfolio();
        repository.loadInitialPortfolio(portfolio);
        PortfolioService portfolioService = new PortfolioService(portfolio, market);
        AnalyticsService analytics = new AnalyticsService(market);

        TradeReceipt buy = portfolioService.placeMarketOrder(OrderType.BUY, "TSLA", 3.0);
        assertEquals(5000.0 - buy.getNetAmount(), portfolio.getCashBalance(), 0.001);
        assertInstanceOf(Stock.class, portfolioService.findAssetBySymbol("TSLA").orElseThrow());

        market.simulateMarketDay();
        double valueAfterSimulation = analytics.calculateTotalValue(portfolio);
        assertTrue(valueAfterSimulation > 0.0);

        double cashBeforeSale = portfolio.getCashBalance();
        TradeReceipt sell = portfolioService.placeMarketOrder(OrderType.SELL, "MSFT", 8.0);
        assertTrue(portfolioService.findAssetBySymbol("MSFT").isEmpty());
        assertEquals(cashBeforeSale + sell.getNetAmount(), portfolio.getCashBalance(), 0.001);

        repository.save(portfolio);
        Portfolio restored = new Portfolio();
        repository.loadSavedState(restored);

        assertEquals(portfolio.getCashBalance(), restored.getCashBalance(), 0.01);
        assertEquals(portfolio.getAssets().size(), restored.getAssets().size());
        Asset tesla = restored.findAsset("TSLA").orElseThrow();
        assertEquals(3.0, tesla.getQuantity(), 0.0001);
    }
}
