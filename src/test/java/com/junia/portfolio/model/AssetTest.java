package com.junia.portfolio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetTest {
    private Stock stock;
    private CryptoAsset crypto;
    private Bond bond;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        stock = new Stock("AAPL", 10.0, 150.0, 0.005);
        crypto = new CryptoAsset("BTC", 0.5, 60000.0, 15.0);
        bond = new Bond("US10Y", 5.0, 1000.0, 0.04, 2034);
        portfolio = new Portfolio(10000.0);
    }

    @Test
    void shouldCalculateCurrentValueForStock() {
        double value = stock.calculateCurrentValue(180.0);
        assertEquals(1800.0, value, 0.001);
    }

    @Test
    void shouldCalculateCurrentValueForCrypto() {
        double value = crypto.calculateCurrentValue(70000.0);
        assertEquals(35000.0, value, 0.001);
    }

    @Test
    void shouldCalculateCurrentValueForBond() {
        double value = bond.calculateCurrentValue(1050.0);
        assertEquals(5250.0, value, 0.001);
    }

    @Test
    void shouldAddAssetsToPortfolio() {
        portfolio.addAsset(stock);
        portfolio.addAsset(crypto);
        portfolio.addAsset(bond);

        assertEquals(3, portfolio.getAssets().size());
        assertEquals(10000.0, portfolio.getCashBalance(), 0.001);
    }

    @Test
    void shouldCalculateTaxableGains() {
        assertEquals(300.0, stock.calculateTaxableGain(180.0, 10.0), 0.001);
        assertEquals(0.0, stock.calculateTaxableGain(120.0, 10.0), 0.001);
        assertEquals(0.30, stock.getTaxRate(), 0.001);
        assertEquals(0.15, bond.getTaxRate(), 0.001);
    }

    @Test
    void shouldValidateOrderSizes() {
        assertTrue(stock.isOrderSizeValid(5.0));
        assertFalse(stock.isOrderSizeValid(5.5));
        assertFalse(stock.isOrderSizeValid(-1.0));

        assertTrue(crypto.isOrderSizeValid(0.0005));
        assertTrue(bond.isOrderSizeValid(2.0));
        assertFalse(bond.isOrderSizeValid(0.5));
    }
}
