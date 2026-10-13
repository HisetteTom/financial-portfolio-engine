package com.junia.portfolio.model;

public class Bond extends Asset {
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
