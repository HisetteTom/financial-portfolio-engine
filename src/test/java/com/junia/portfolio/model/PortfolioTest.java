package com.junia.portfolio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioTest {
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(10000.0);
        portfolio.addAsset(new Stock("AAPL", 10.0, 150.0, 0.005));
    }

    @Test
    void shouldFindAssetIgnoringCase() {
        assertTrue(portfolio.findAsset("aapl").isPresent());
        assertTrue(portfolio.findAsset("TSLA").isEmpty());
    }

    @Test
    void shouldRefuseTwoPositionsWithSameSymbol() {
        Stock duplicate = new Stock("AAPL", 5.0, 160.0, 0.005);

        assertThrows(IllegalArgumentException.class, () -> portfolio.addAsset(duplicate));
        assertEquals(1, portfolio.getAssets().size());
    }

    @Test
    void shouldNotAllowAssetListToBeModifiedFromOutside() {
        Stock other = new Stock("MSFT", 1.0, 300.0, 0.008);

        assertThrows(UnsupportedOperationException.class, () -> portfolio.getAssets().add(other));
    }

    @Test
    void shouldRefuseNegativeCashBalance() {
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(-1.0));
        assertThrows(IllegalArgumentException.class, () -> portfolio.setCashBalance(-50.0));
    }

    @Test
    void shouldEmptyAssetsAndCashWhenCleared() {
        portfolio.clear();

        assertTrue(portfolio.getAssets().isEmpty());
        assertEquals(0.0, portfolio.getCashBalance(), 0.001);
    }
}
