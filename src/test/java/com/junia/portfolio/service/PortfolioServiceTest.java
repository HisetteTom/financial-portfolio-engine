package com.junia.portfolio.service;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Order;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Stock;
import com.junia.portfolio.utils.AssetCsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortfolioServiceTest {
    private Portfolio portfolio;
    private PortfolioService service;
    private Stock appleStock;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(1000.0);
        service = new PortfolioService(portfolio, new AssetCsvParser());
        appleStock = new Stock("AAPL", 10.0, 100.0, 0.01);
        portfolio.addAsset(appleStock);
    }

    @Test
    void shouldCalculateTotalPortfolioValue() {
        Map<String, Double> prices = new HashMap<>();
        prices.put("AAPL", 150.0);

        double total = service.calculateTotalValue(prices);
        assertEquals(2500.0, total, 0.001);
    }

    @Test
    void shouldExecuteBuyOrderSuccessfully() throws Exception {
        Order buyOrder = new Order("AAPL", OrderType.BUY, 5.0, 100.0);
        service.executeOrder(buyOrder);

        assertEquals(500.0, portfolio.getCashBalance(), 0.001);
        Asset asset = service.findAssetBySymbol("AAPL").orElseThrow();
        assertEquals(15.0, asset.getQuantity(), 0.001);
    }

    @Test
    void shouldThrowExceptionWhenFundsAreInsufficient() {
        Order buyOrder = new Order("AAPL", OrderType.BUY, 20.0, 100.0);
        assertThrows(InsufficientFundsException.class, () -> service.executeOrder(buyOrder));
    }

    @Test
    void shouldExecuteSellOrderSuccessfully() throws Exception {
        Order sellOrder = new Order("AAPL", OrderType.SELL, 4.0, 120.0);
        service.executeOrder(sellOrder);

        assertEquals(1480.0, portfolio.getCashBalance(), 0.001);
        Asset asset = service.findAssetBySymbol("AAPL").orElseThrow();
        assertEquals(6.0, asset.getQuantity(), 0.001);
    }

    @Test
    void shouldThrowExceptionWhenSellingMoreThanOwned() {
        Order sellOrder = new Order("AAPL", OrderType.SELL, 20.0, 120.0);
        assertThrows(InvalidTransactionException.class, () -> service.executeOrder(sellOrder));
    }

    @Test
    void shouldThrowExceptionWhenAssetNotFound() {
        Order sellOrder = new Order("TSLA", OrderType.SELL, 1.0, 200.0);
        assertThrows(AssetNotFoundException.class, () -> service.executeOrder(sellOrder));
    }
}
