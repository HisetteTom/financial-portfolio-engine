package com.junia.portfolio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataServiceTest {
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        Map<String, Double> initial = new HashMap<>();
        initial.put("AAPL", 150.0);
        initial.put("BTC", 50000.0);
        marketDataService = new MarketDataService(initial);
    }

    @Test
    void shouldReturnCorrectInitialPrice() {
        assertEquals(150.0, marketDataService.getPrice("AAPL"), 0.001);
        assertEquals(50000.0, marketDataService.getPrice("BTC"), 0.001);
        assertEquals(0.0, marketDataService.getPrice("UNKNOWN"), 0.001);
    }

    @Test
    void shouldUpdatePriceAndRecordHistory() {
        marketDataService.setPrice("AAPL", 160.0);
        assertEquals(160.0, marketDataService.getPrice("AAPL"), 0.001);

        List<Double> history = marketDataService.getPriceHistory("AAPL");
        assertEquals(2, history.size());
        assertEquals(150.0, history.get(0), 0.001);
        assertEquals(160.0, history.get(1), 0.001);
    }

    @Test
    void shouldSimulateMarketDayAndAppendHistory() {
        marketDataService.simulateMarketDay();

        List<Double> aaplHistory = marketDataService.getPriceHistory("AAPL");
        assertEquals(2, aaplHistory.size());
        assertFalse(aaplHistory.get(1) <= 0.0);
        assertTrue(marketDataService.getPrice("AAPL") > 0.0);
    }
}
