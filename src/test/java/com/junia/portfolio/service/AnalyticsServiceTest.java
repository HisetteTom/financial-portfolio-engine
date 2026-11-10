package com.junia.portfolio.service;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.AssetType;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.PositionSummary;
import com.junia.portfolio.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsServiceTest {
    private AnalyticsService analyticsService;
    private Portfolio portfolio;
    private Stock stock;
    private CryptoAsset crypto;
    private Bond bond;

    @BeforeEach
    void setUp() {
        List<Asset> listings = List.of(
                new Stock("AAPL", 0.0, 150.0, 0.01),
                new CryptoAsset("BTC", 0.0, 40000.0, 10.0),
                new Bond("US10Y", 0.0, 1100.0, 0.04, 2034));
        analyticsService = new AnalyticsService(new MarketDataService(listings, new Random(42)));

        stock = new Stock("AAPL", 10.0, 100.0, 0.01);
        crypto = new CryptoAsset("BTC", 1.0, 50000.0, 10.0);
        bond = new Bond("US10Y", 10.0, 1000.0, 0.04, 2034);

        portfolio = new Portfolio(1000.0);
        portfolio.addAsset(stock);
        portfolio.addAsset(crypto);
        portfolio.addAsset(bond);
    }

    @Test
    void shouldCalculateTotalValueIncludingCash() {
        assertEquals(53500.0, analyticsService.calculateTotalValue(portfolio), 0.001);
    }

    @Test
    void shouldUsePurchasePriceWhenAssetHasNoMarketPrice() {
        portfolio.addAsset(new Stock("XYZ", 2.0, 50.0, 0.0));

        assertEquals(53600.0, analyticsService.calculateTotalValue(portfolio), 0.001);
    }

    @Test
    void shouldCalculateUnrealizedGainAndLoss() {
        assertEquals(500.0, analyticsService.calculateUnrealizedGainLoss(stock), 0.001);
        assertEquals(-10000.0, analyticsService.calculateUnrealizedGainLoss(crypto), 0.001);
        assertEquals(-8500.0, analyticsService.calculateTotalUnrealizedGainLoss(portfolio), 0.001);
    }

    @Test
    void shouldCalculateTotalCostBasis() {
        assertEquals(61000.0, analyticsService.calculateTotalCostBasis(portfolio), 0.001);
    }

    @Test
    void shouldCalculateReturnOnInvestment() {
        assertEquals(50.0, analyticsService.calculateReturnOnInvestment(stock), 0.001);
        assertEquals(-20.0, analyticsService.calculateReturnOnInvestment(crypto), 0.001);
    }

    @Test
    void shouldSumAnnualIncomeOfAllAssetTypes() {
        assertEquals(415.0, analyticsService.calculateProjectedAnnualIncome(portfolio), 0.001);
    }

    @Test
    void shouldEstimateSellingTaxOnlyForGains() {
        assertEquals(75.0, analyticsService.estimateSellingTax(stock, 5.0), 0.001);
        assertEquals(30.0, analyticsService.estimateSellingTax(bond, 2.0), 0.001);
        assertEquals(0.0, analyticsService.estimateSellingTax(crypto, 1.0), 0.001);
    }

    @Test
    void shouldCalculateAllocationPercentage() {
        assertEquals(50.0, analyticsService.calculateAllocationPercentage(stock, 3000.0), 0.001);
        assertEquals(0.0, analyticsService.calculateAllocationPercentage(stock, 0.0), 0.001);
    }

    @Test
    void shouldSummarizeEveryPosition() {
        List<PositionSummary> positions = analyticsService.summarizePositions(portfolio);

        assertEquals(3, positions.size());
        PositionSummary apple = positions.get(0);
        assertEquals("AAPL", apple.getSymbol());
        assertEquals(AssetType.STOCK, apple.getAssetType());
        assertEquals(1500.0, apple.getCurrentValue(), 0.001);
        assertEquals(15.0, apple.getAnnualIncome(), 0.001);
        assertEquals(1500.0 / 53500.0 * 100.0, apple.getAllocation(), 0.001);
    }
}
