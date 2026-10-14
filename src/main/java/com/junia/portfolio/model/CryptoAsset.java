package com.junia.portfolio.model;

public class CryptoAsset extends Asset implements Taxable, Tradeable {
    private double networkGasFee;

    public CryptoAsset(String symbol, double quantity, double purchasePrice, double networkGasFee) {
        super(symbol, quantity, purchasePrice);
        this.networkGasFee = networkGasFee;
    }

    @Override
    public double calculateCurrentValue(double currentPrice) {
        return getQuantity() * currentPrice;
    }

    @Override
    public double calculateTaxableGain(double sellingPrice, double unitsSold) {
        double gain = (sellingPrice - getPurchasePrice()) * unitsSold;
        return Math.max(0.0, gain);
    }

    @Override
    public double getTaxRate() {
        return 0.30;
    }

    @Override
    public boolean isOrderSizeValid(double quantity) {
        return quantity >= 0.00000001;
    }

    public double getNetworkGasFee() {
        return networkGasFee;
    }

    public void setNetworkGasFee(double networkGasFee) {
        this.networkGasFee = networkGasFee;
    }
}
