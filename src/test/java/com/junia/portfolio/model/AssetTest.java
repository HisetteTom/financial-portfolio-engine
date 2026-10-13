package com.junia.portfolio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssetTest {
    private Stock stock;
    private CryptoAsset crypto;
    private Bond bond;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        stock = new Stock("YOLO", 10.0, 150.0, 0.005);
        crypto = new CryptoAsset("BTC", 0.5, 60000.0, 15.0);
        bond = new Bond("FR-10%", 5.0, 1000.0, 0.04, 2034);
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
}
