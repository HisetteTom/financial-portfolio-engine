package com.junia.portfolio.model;

public class Stock extends Asset {
    private double annualDividendRate;

    public Stock(String symbol, double quantity, double purchasePrice, double annualDividendRate) {
        super(symbol, quantity, purchasePrice);
        this.annualDividendRate = annualDividendRate;
    }

    @Override
    public double calculateCurrentValue(double currentPrice) {
        return getQuantity() * currentPrice;
    }

    public double getAnnualDividendRate() {
        return annualDividendRate;
    }

    public void setAnnualDividendRate(double annualDividendRate) {
        this.annualDividendRate = annualDividendRate;
    }
}
