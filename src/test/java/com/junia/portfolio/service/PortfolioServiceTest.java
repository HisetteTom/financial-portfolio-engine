package com.junia.portfolio.service;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Order;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Stock;
import com.junia.portfolio.model.TradeReceipt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioServiceTest {
    private Portfolio portfolio;
    private PortfolioService service;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(10000.0);
        portfolio.addAsset(new Stock("AAPL", 10.0, 100.0, 0.01));

        List<Asset> listings = List.of(
                new Stock("AAPL", 0.0, 120.0, 0.01),
                new Stock("TSLA", 0.0, 200.0, 0.0),
                new CryptoAsset("BTC", 0.0, 50000.0, 10.0),
                new Bond("US10Y", 0.0, 1000.0, 0.04, 2034));
        MarketDataService marketDataService = new MarketDataService(listings, new Random(42));

        service = new PortfolioService(portfolio, marketDataService);
    }

    @Test
    void shouldBuyExistingAssetAndPayFees() throws Exception {
        TradeReceipt receipt = service.executeOrder(new Order("AAPL", OrderType.BUY, 5.0, 100.0));

        assertEquals(1.5, receipt.getFee(), 0.001);
        assertEquals(9498.5, portfolio.getCashBalance(), 0.001);
        assertEquals(15.0, service.findAssetBySymbol("AAPL").orElseThrow().getQuantity(), 0.001);
    }

    @Test
    void shouldUpdateAveragePurchasePriceWhenBuyingMore() throws Exception {
        service.executeOrder(new Order("AAPL", OrderType.BUY, 10.0, 120.0));

        Asset apple = service.findAssetBySymbol("AAPL").orElseThrow();
        assertEquals(20.0, apple.getQuantity(), 0.001);
        assertEquals(110.0, apple.getPurchasePrice(), 0.001);
    }

    @Test
    void shouldCreateNewPositionWhenBuyingAssetNotYetOwned() throws Exception {
        service.executeOrder(new Order("TSLA", OrderType.BUY, 2.0, 200.0));

        Asset tesla = service.findAssetBySymbol("TSLA").orElseThrow();
        assertInstanceOf(Stock.class, tesla);
        assertEquals(2.0, tesla.getQuantity(), 0.001);
        assertEquals(200.0, tesla.getPurchasePrice(), 0.001);
        assertEquals(10000.0 - 401.4, portfolio.getCashBalance(), 0.001);
        assertEquals(2, portfolio.getAssets().size());
    }

    @Test
    void shouldCreateCryptoPositionWithFractionalQuantity() throws Exception {
        TradeReceipt receipt = service.executeOrder(new Order("BTC", OrderType.BUY, 0.1, 50000.0));

        assertInstanceOf(CryptoAsset.class, service.findAssetBySymbol("BTC").orElseThrow());
        assertEquals(35.0, receipt.getFee(), 0.001);
        assertEquals(10000.0 - 5035.0, portfolio.getCashBalance(), 0.001);
    }

    @Test
    void shouldThrowExceptionWhenBuyingUnknownSymbol() {
        Order order = new Order("XYZ", OrderType.BUY, 1.0, 10.0);

        assertThrows(AssetNotFoundException.class, () -> service.executeOrder(order));
        assertEquals(10000.0, portfolio.getCashBalance(), 0.001);
    }

    @Test
    void shouldThrowExceptionWhenFundsAreInsufficient() {
        Order order = new Order("AAPL", OrderType.BUY, 100.0, 100.0);

        assertThrows(InsufficientFundsException.class, () -> service.executeOrder(order));
        assertEquals(10000.0, portfolio.getCashBalance(), 0.001);
        assertEquals(10.0, service.findAssetBySymbol("AAPL").orElseThrow().getQuantity(), 0.001);
    }

    @Test
    void shouldRefuseBuyWhenFeesExceedRemainingCash() {
        portfolio.setCashBalance(1000.0);
        Order order = new Order("US10Y", OrderType.BUY, 1.0, 1000.0);

        assertThrows(InsufficientFundsException.class, () -> service.executeOrder(order));
    }

    @Test
    void shouldAllowBuyUsingExactlyAllAvailableCash() throws Exception {
        portfolio.setCashBalance(1005.0);

        service.executeOrder(new Order("US10Y", OrderType.BUY, 1.0, 1000.0));

        assertEquals(0.0, portfolio.getCashBalance(), 0.001);
    }

    @Test
    void shouldRefuseFractionalStockQuantity() {
        Order order = new Order("AAPL", OrderType.BUY, 2.5, 100.0);

        assertThrows(InvalidTransactionException.class, () -> service.executeOrder(order));
    }

    @Test
    void shouldRefuseZeroOrNegativeOrders() {
        assertThrows(InvalidTransactionException.class, () -> service.executeOrder(new Order("AAPL", OrderType.BUY, 0.0, 100.0)));
        assertThrows(InvalidTransactionException.class, () -> service.executeOrder(new Order("AAPL", OrderType.SELL, -1.0, 100.0)));
        assertThrows(InvalidTransactionException.class, () -> service.executeOrder(new Order("AAPL", OrderType.BUY, 1.0, 0.0)));
    }

    @Test
    void shouldDeductFeesAndTaxWhenSellingWithGain() throws Exception {
        TradeReceipt receipt = service.executeOrder(new Order("AAPL", OrderType.SELL, 4.0, 120.0));

        assertEquals(1.48, receipt.getFee(), 0.001);
        assertEquals(24.0, receipt.getTax(), 0.001);
        assertEquals(454.52, receipt.getNetAmount(), 0.001);
        assertEquals(10454.52, portfolio.getCashBalance(), 0.001);
        assertEquals(6.0, service.findAssetBySymbol("AAPL").orElseThrow().getQuantity(), 0.001);
    }

    @Test
    void shouldNotTaxSaleAtLoss() throws Exception {
        TradeReceipt receipt = service.executeOrder(new Order("AAPL", OrderType.SELL, 4.0, 80.0));

        assertEquals(0.0, receipt.getTax(), 0.001);
        assertEquals(318.68, receipt.getNetAmount(), 0.001);
    }

    @Test
    void shouldRemovePositionWhenEverythingIsSold() throws Exception {
        service.executeOrder(new Order("AAPL", OrderType.SELL, 10.0, 120.0));

        assertTrue(service.findAssetBySymbol("AAPL").isEmpty());
        assertTrue(portfolio.getAssets().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenSellingMoreThanOwned() {
        Order order = new Order("AAPL", OrderType.SELL, 20.0, 120.0);

        assertThrows(InvalidTransactionException.class, () -> service.executeOrder(order));
        assertEquals(10.0, service.findAssetBySymbol("AAPL").orElseThrow().getQuantity(), 0.001);
    }

    @Test
    void shouldThrowExceptionWhenSellingAssetNotOwned() {
        Order order = new Order("TSLA", OrderType.SELL, 1.0, 200.0);

        assertThrows(AssetNotFoundException.class, () -> service.executeOrder(order));
    }

    @Test
    void shouldRefuseSaleThatDoesNotCoverFees() {
        portfolio.addAsset(new Bond("US10Y", 1.0, 1000.0, 0.04, 2034));
        Order order = new Order("US10Y", OrderType.SELL, 1.0, 3.0);

        assertThrows(InvalidTransactionException.class, () -> service.executeOrder(order));
        assertEquals(10000.0, portfolio.getCashBalance(), 0.001);
    }

    @Test
    void shouldUseCurrentMarketPriceForMarketOrders() throws Exception {
        TradeReceipt receipt = service.placeMarketOrder(OrderType.BUY, "tsla", 1.0);

        assertEquals("TSLA", receipt.getOrder().getSymbol());
        assertEquals(200.0, receipt.getOrder().getPrice(), 0.001);
        assertTrue(service.findAssetBySymbol("TSLA").isPresent());
    }

    @Test
    void shouldRefuseMarketOrderWithoutSymbol() {
        assertThrows(InvalidTransactionException.class, () -> service.placeMarketOrder(OrderType.BUY, " ", 1.0));
    }

    @Test
    void shouldThrowExceptionForMarketOrderOnUnlistedSymbol() {
        assertThrows(AssetNotFoundException.class, () -> service.placeMarketOrder(OrderType.BUY, "XYZ", 1.0));
    }
}
