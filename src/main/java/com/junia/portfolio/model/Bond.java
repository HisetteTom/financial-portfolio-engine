package com.junia.portfolio.model;

import java.util.List;

public class Bond extends Asset {
    // the coupon is paid on the face value, not on the market price
    private static final double FACE_VALUE = 1000.0;
    private static final double ADMINISTRATIVE_FEE = 5.0;
    private static final double TAX_RATE = 0.15;

    private final double couponRate;
    private final int maturityYear;

    public Bond(String symbol, double quantity, double purchasePrice, double couponRate, int maturityYear) {
        super(symbol, quantity, purchasePrice);
        if (couponRate < 0) {
            throw new IllegalArgumentException("Coupon rate cannot be negative for " + symbol + ".");
        }
        if (maturityYear <= 0) {
            throw new IllegalArgumentException("Maturity year is invalid for " + symbol + ".");
        }
        this.couponRate = couponRate;
        this.maturityYear = maturityYear;
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.BOND;
    }

    @Override
    public double calculateTransactionFee(double orderQuantity, double price) {
        return ADMINISTRATIVE_FEE;
    }

    @Override
    public double calculateAnnualIncome(double currentPrice) {
        return getQuantity() * FACE_VALUE * couponRate;
    }

    @Override
    public Asset createPosition(double quantity, double purchasePrice) {
        return new Bond(getSymbol(), quantity, purchasePrice, couponRate, maturityYear);
    }

    @Override
    public List<Double> getSpecificParameters() {
        return List.of(couponRate, (double) maturityYear);
    }

    @Override
    public double getTaxRate() {
        return TAX_RATE;
    }

    @Override
    public boolean isOrderSizeValid(double quantity) {
        return quantity >= 1.0 && quantity == Math.floor(quantity);
    }

    public double getCouponRate() {
        return couponRate;
    }

    public int getMaturityYear() {
        return maturityYear;
    }
}
