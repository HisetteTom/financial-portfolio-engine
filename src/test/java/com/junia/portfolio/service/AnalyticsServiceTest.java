package com.junia.portfolio.service;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsServiceTest {
    private AnalyticsService analyticsService;
    private Stock stock;
    private CryptoAsset crypto;
    private Bond bond;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService();
        stock = new Stock("AAPL", 10.0, 100.0, 0.01);
        crypto = new CryptoAsset("BTC", 1.0, 50000.0, 10.0);
        bond = new Bond("US10Y", 10.0, 1000.0, 0.04, 2034);

        portfolio = new Portfolio(1000.0);
        portfolio.addAsset(stock);
    }

    @Test
    void shouldCalculateUnrealizedGainLoss() {
        double gain = analyticsService.calculateUnrealizedGainLoss(stock, 150.0);
        assertEquals(500.0, gain, 0.001);

        double loss = analyticsService.calculateUnrealizedGainLoss(stock, 80.0);
        assertEquals(-200.0, loss, 0.001);
    }

    @Test
    void shouldCalculateReturnOnInvestment() {
        double roi = analyticsService.calculateReturnOnInvestment(stock, 150.0);
        assertEquals(50.0, roi, 0.001);

        double negativeRoi = analyticsService.calculateReturnOnInvestment(stock, 75.0);
        assertEquals(-25.0, negativeRoi, 0.001);
    }

    @Test
    void shouldEstimateSellingTaxes() {
        double stockTax = analyticsService.estimateSellingTax(stock, 150.0, 5.0);
        assertEquals(75.0, stockTax, 0.001);

        double bondTax = analyticsService.estimateSellingTax(bond, 1100.0, 2.0);
        assertEquals(30.0, bondTax, 0.001);

        double noGainTax = analyticsService.estimateSellingTax(stock, 90.0, 5.0);
        assertEquals(0.0, noGainTax, 0.001);
    }

    @Test
    void shouldCalculateAssetAllocationPercentage() {
        double allocation = analyticsService.calculateAssetAllocationPercentage(portfolio, stock, 100.0, 2000.0);
        assertEquals(50.0, allocation, 0.001);
    }
}
