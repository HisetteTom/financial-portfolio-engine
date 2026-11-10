package com.junia.portfolio.model;

import java.util.List;

public class Stock extends Asset {
    private static final double BROKERAGE_FEE = 1.0;
    private static final double VOLUME_FEE_RATE = 0.001;
    private static final double FLAT_TAX_RATE = 0.30;

    private final double annualDividendRate;

    public Stock(String symbol, double quantity, double purchasePrice, double annualDividendRate) {
        super(symbol, quantity, purchasePrice);
        if (annualDividendRate < 0) {
            throw new IllegalArgumentException("Dividend rate cannot be negative for " + symbol + ".");
        }
        this.annualDividendRate = annualDividendRate;
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.STOCK;
    }

    @Override
    public double calculateTransactionFee(double orderQuantity, double price) {
        return BROKERAGE_FEE + orderQuantity * price * VOLUME_FEE_RATE;
    }

    @Override
    public double calculateAnnualIncome(double currentPrice) {
        return getQuantity() * currentPrice * annualDividendRate;
    }

    @Override
    public Asset createPosition(double quantity, double purchasePrice) {
        return new Stock(getSymbol(), quantity, purchasePrice, annualDividendRate);
    }

    @Override
    public List<Double> getSpecificParameters() {
        return List.of(annualDividendRate);
    }

    @Override
    public double getTaxRate() {
        return FLAT_TAX_RATE;
    }

    @Override
    public boolean isOrderSizeValid(double quantity) {
        return quantity > 0 && quantity == Math.floor(quantity);
    }

    public double getAnnualDividendRate() {
        return annualDividendRate;
    }
}
