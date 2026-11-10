package com.junia.portfolio.model;

import java.util.List;

public class CryptoAsset extends Asset {
    private static final double LIQUIDITY_SPREAD_RATE = 0.005;
    private static final double FLAT_TAX_RATE = 0.30;
    private static final double SMALLEST_UNIT = 0.00000001;

    private final double networkGasFee;

    public CryptoAsset(String symbol, double quantity, double purchasePrice, double networkGasFee) {
        super(symbol, quantity, purchasePrice);
        if (networkGasFee < 0) {
            throw new IllegalArgumentException("Gas fee cannot be negative for " + symbol + ".");
        }
        this.networkGasFee = networkGasFee;
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.CRYPTO;
    }

    @Override
    public double calculateTransactionFee(double orderQuantity, double price) {
        return networkGasFee + orderQuantity * price * LIQUIDITY_SPREAD_RATE;
    }

    @Override
    public double calculateAnnualIncome(double currentPrice) {
        return 0.0;
    }

    @Override
    public Asset createPosition(double quantity, double purchasePrice) {
        return new CryptoAsset(getSymbol(), quantity, purchasePrice, networkGasFee);
    }

    @Override
    public List<Double> getSpecificParameters() {
        return List.of(networkGasFee);
    }

    @Override
    public double getTaxRate() {
        return FLAT_TAX_RATE;
    }

    @Override
    public boolean isOrderSizeValid(double quantity) {
        return quantity >= SMALLEST_UNIT;
    }

    public double getNetworkGasFee() {
        return networkGasFee;
    }
}
