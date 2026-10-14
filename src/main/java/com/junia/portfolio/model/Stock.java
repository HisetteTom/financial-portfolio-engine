package com.junia.portfolio.model;

public class Stock extends Asset implements Taxable, Tradeable {
    private double annualDividendRate;

    public Stock(String symbol, double quantity, double purchasePrice, double annualDividendRate) {
        super(symbol, quantity, purchasePrice);
        this.annualDividendRate = annualDividendRate;
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
        return quantity > 0 && quantity == Math.floor(quantity);
    }

    public double getAnnualDividendRate() {
        return annualDividendRate;
    }

    public void setAnnualDividendRate(double annualDividendRate) {
        this.annualDividendRate = annualDividendRate;
    }
}
