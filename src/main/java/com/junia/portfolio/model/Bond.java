package com.junia.portfolio.model;

public class Bond extends Asset implements Taxable, Tradeable {
    private double couponRate;
    private int maturityYear;

    public Bond(String symbol, double quantity, double purchasePrice, double couponRate, int maturityYear) {
        super(symbol, quantity, purchasePrice);
        this.couponRate = couponRate;
        this.maturityYear = maturityYear;
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
        return 0.15;
    }

    @Override
    public boolean isOrderSizeValid(double quantity) {
        return quantity >= 1.0 && quantity == Math.floor(quantity);
    }

    public double getCouponRate() {
        return couponRate;
    }

    public void setCouponRate(double couponRate) {
        this.couponRate = couponRate;
    }

    public int getMaturityYear() {
        return maturityYear;
    }

    public void setMaturityYear(int maturityYear) {
        this.maturityYear = maturityYear;
    }
}
