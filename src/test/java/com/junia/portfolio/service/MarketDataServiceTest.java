package com.junia.portfolio.service;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataServiceTest {
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        List<Asset> listings = List.of(
                new Stock("AAPL", 0.0, 150.0, 0.005),
                new CryptoAsset("BTC", 0.0, 50000.0, 15.0),
                new Bond("US10Y", 0.0, 1000.0, 0.045, 2034));
        marketDataService = new MarketDataService(listings, new Random(42));
    }

    @Test
    void shouldStartWithListingPrices() {
        assertEquals(150.0, marketDataService.findPrice("AAPL").orElseThrow(), 0.001);
        assertEquals(50000.0, marketDataService.findPrice("btc").orElseThrow(), 0.001);
        assertTrue(marketDataService.findPrice("UNKNOWN").isEmpty());
    }

    @Test
    void shouldFindListingWithItsType() {
        assertInstanceOf(Bond.class, marketDataService.findListing("US10Y").orElseThrow());
        assertTrue(marketDataService.findListing("TSLA").isEmpty());
    }

    @Test
    void shouldFallBackToPurchasePriceForUnlistedAsset() {
        Stock unlisted = new Stock("XYZ", 1.0, 42.0, 0.0);

        assertEquals(42.0, marketDataService.getCurrentPrice(unlisted), 0.001);
    }

    @Test
    void shouldUpdatePriceAndRecordHistory() {
        marketDataService.setPrice("AAPL", 160.0);

        List<Double> history = marketDataService.getPriceHistory("AAPL");
        assertEquals(160.0, marketDataService.findPrice("AAPL").orElseThrow(), 0.001);
        assertEquals(2, history.size());
        assertEquals(150.0, history.get(0), 0.001);
        assertEquals(160.0, history.get(1), 0.001);
    }

    @Test
    void shouldRefuseNonPositivePrice() {
        assertThrows(IllegalArgumentException.class, () -> marketDataService.setPrice("AAPL", 0.0));
    }

    @Test
    void shouldKeepDailyVariationWithinFourPercent() {
        marketDataService.simulateMarketDay();

        double newPrice = marketDataService.findPrice("AAPL").orElseThrow();
        assertTrue(newPrice >= 150.0 * 0.96 - 0.01);
        assertTrue(newPrice <= 150.0 * 1.04 + 0.01);
        assertEquals(2, marketDataService.getPriceHistory("AAPL").size());
    }

    @Test
    void shouldAppendOnePricePerSimulatedDay() {
        for (int day = 0; day < 30; day++) {
            marketDataService.simulateMarketDay();
        }

        assertEquals(31, marketDataService.getPriceHistory("BTC").size());
        assertTrue(marketDataService.findPrice("BTC").orElseThrow() > 0.0);
    }
}
