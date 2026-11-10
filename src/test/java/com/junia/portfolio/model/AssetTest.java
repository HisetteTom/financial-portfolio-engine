package com.junia.portfolio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetTest {
    private Stock stock;
    private CryptoAsset crypto;
    private Bond bond;

    @BeforeEach
    void setUp() {
        stock = new Stock("AAPL", 10.0, 150.0, 0.005);
        crypto = new CryptoAsset("BTC", 0.5, 60000.0, 15.0);
        bond = new Bond("US10Y", 5.0, 1000.0, 0.04, 2034);
    }

    @Test
    void shouldCalculateCurrentValueFromMarketPrice() {
        assertEquals(1800.0, stock.calculateCurrentValue(180.0), 0.001);
        assertEquals(35000.0, crypto.calculateCurrentValue(70000.0), 0.001);
        assertEquals(5250.0, bond.calculateCurrentValue(1050.0), 0.001);
    }

    @Test
    void shouldApplyDifferentFeeRulesDependingOnAssetType() {
        List<Asset> assets = List.of(stock, crypto, bond);

        List<Double> fees = assets.stream()
                .map(asset -> asset.calculateTransactionFee(1.0, 1000.0))
                .toList();

        assertEquals(2.0, fees.get(0), 0.001);
        assertEquals(20.0, fees.get(1), 0.001);
        assertEquals(5.0, fees.get(2), 0.001);
    }

    @Test
    void shouldIncreaseStockFeeWithOrderVolume() {
        assertEquals(2.0, stock.calculateTransactionFee(10.0, 100.0), 0.001);
        assertEquals(11.0, stock.calculateTransactionFee(100.0, 100.0), 0.001);
    }

    @Test
    void shouldKeepBondFeeFlatWhateverTheVolume() {
        assertEquals(5.0, bond.calculateTransactionFee(1.0, 1000.0), 0.001);
        assertEquals(5.0, bond.calculateTransactionFee(50.0, 1000.0), 0.001);
    }

    @Test
    void shouldCalculateAnnualIncomeDependingOnAssetType() {
        assertEquals(9.0, stock.calculateAnnualIncome(180.0), 0.001);
        assertEquals(200.0, bond.calculateAnnualIncome(1050.0), 0.001);
        assertEquals(0.0, crypto.calculateAnnualIncome(70000.0), 0.001);
    }

    @Test
    void shouldCalculateTaxableGainOnlyWhenSellingAboveCost() {
        assertEquals(300.0, stock.calculateTaxableGain(180.0, 10.0), 0.001);
        assertEquals(0.0, stock.calculateTaxableGain(120.0, 10.0), 0.001);
    }

    @Test
    void shouldApplyTaxRateOfEachAssetType() {
        assertEquals(90.0, stock.calculateSellingTax(180.0, 10.0), 0.001);
        assertEquals(15.0, bond.calculateSellingTax(1100.0, 1.0), 0.001);
        assertEquals(0.30, crypto.getTaxRate(), 0.001);
    }

    @Test
    void shouldValidateOrderSizes() {
        assertTrue(stock.isOrderSizeValid(5.0));
        assertFalse(stock.isOrderSizeValid(5.5));
        assertFalse(stock.isOrderSizeValid(-1.0));

        assertTrue(crypto.isOrderSizeValid(0.0005));
        assertTrue(crypto.isOrderSizeValid(0.00000001));
        assertFalse(crypto.isOrderSizeValid(0.000000001));

        assertTrue(bond.isOrderSizeValid(2.0));
        assertFalse(bond.isOrderSizeValid(0.5));
    }

    @Test
    void shouldUpdateWeightedAveragePriceWhenAddingUnits() {
        stock.addUnits(10.0, 170.0);

        assertEquals(20.0, stock.getQuantity(), 0.001);
        assertEquals(160.0, stock.getPurchasePrice(), 0.001);
    }

    @Test
    void shouldBeSoldOutAfterRemovingAllUnits() {
        crypto.removeUnits(0.5);

        assertTrue(crypto.isSoldOut());
        assertEquals(0.0, crypto.getQuantity(), 0.001);
    }

    @Test
    void shouldCreateNewPositionWithSameTypeAndParameters() {
        CryptoAsset listing = new CryptoAsset("ETH", 0.0, 3000.0, 6.5);

        Asset position = listing.createPosition(2.0, 3100.0);

        CryptoAsset created = assertInstanceOf(CryptoAsset.class, position);
        assertEquals("ETH", created.getSymbol());
        assertEquals(2.0, created.getQuantity(), 0.001);
        assertEquals(3100.0, created.getPurchasePrice(), 0.001);
        assertEquals(6.5, created.getNetworkGasFee(), 0.001);
        assertEquals(0.0, listing.getQuantity(), 0.001);
    }

    @Test
    void shouldExposeTypeAndSpecificParameters() {
        assertEquals(AssetType.STOCK, stock.getAssetType());
        assertEquals(List.of(0.005), stock.getSpecificParameters());
        assertEquals(List.of(15.0), crypto.getSpecificParameters());
        assertEquals(List.of(0.04, 2034.0), bond.getSpecificParameters());
    }

    @Test
    void shouldCreateEachTypeFromItsParameters() {
        Asset createdBond = AssetType.BOND.create("FRTR", 3.0, 980.0, List.of(0.032, 2030.0));

        Bond bond = assertInstanceOf(Bond.class, createdBond);
        assertEquals(2030, bond.getMaturityYear());
        assertInstanceOf(Stock.class, AssetType.STOCK.create("TSLA", 1.0, 200.0, List.of(0.0)));
        assertInstanceOf(CryptoAsset.class, AssetType.CRYPTO.create("SOL", 1.0, 145.0, List.of(0.5)));
    }

    @Test
    void shouldRefuseWrongNumberOfParameters() {
        assertThrows(IllegalArgumentException.class, () -> AssetType.BOND.create("FRTR", 1.0, 980.0, List.of(0.032)));
        assertThrows(IllegalArgumentException.class, () -> AssetType.STOCK.create("AAPL", 1.0, 150.0, List.of()));
    }

    @Test
    void shouldNormalizeSymbolToUpperCase() {
        Stock lowerCase = new Stock(" tsla ", 1.0, 200.0, 0.0);

        assertEquals("TSLA", lowerCase.getSymbol());
    }

    @Test
    void shouldRefuseInvalidAssetData() {
        assertThrows(IllegalArgumentException.class, () -> new Stock("", 1.0, 100.0, 0.01));
        assertThrows(IllegalArgumentException.class, () -> new Stock("AAPL", -1.0, 100.0, 0.01));
        assertThrows(IllegalArgumentException.class, () -> new CryptoAsset("BTC", 1.0, 0.0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> new Bond("US10Y", 1.0, 1000.0, -0.01, 2034));
    }
}
