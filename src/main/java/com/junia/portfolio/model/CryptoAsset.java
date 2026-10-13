package com.junia.portfolio.model;

public class CryptoAsset extends Asset {
    private double networkGasFee;

    public CryptoAsset(String symbol, double quantity, double purchasePrice, double networkGasFee) {
        super(symbol, quantity, purchasePrice);
        this.networkGasFee = networkGasFee;
    }

    @Override
    public double calculateCurrentValue(double currentPrice) {
        return getQuantity() * currentPrice;
    }

    public double getNetworkGasFee() {
        return networkGasFee;
    }

    public void setNetworkGasFee(double networkGasFee) {
        this.networkGasFee = networkGasFee;
    }
}
