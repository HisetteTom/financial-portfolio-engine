package com.junia.portfolio.model;

import java.util.List;

public abstract class Asset implements Taxable, Tradeable {
    // doubles are not exact (0.1 + 0.2 = 0.30000000000000004) so we allow a tiny margin
    private static final double QUANTITY_TOLERANCE = 0.00000001;

    private final String symbol;
    private double quantity;
    private double purchasePrice;

    public Asset(String symbol, double quantity, double purchasePrice) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Asset symbol cannot be empty.");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative for " + symbol + ".");
        }
        if (purchasePrice <= 0) {
            throw new IllegalArgumentException("Price must be positive for " + symbol + ".");
        }
        this.symbol = symbol.trim().toUpperCase();
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
    }

    public abstract AssetType getAssetType();

    public abstract double calculateTransactionFee(double orderQuantity, double price);

    public abstract double calculateAnnualIncome(double currentPrice);

    public abstract Asset createPosition(double quantity, double purchasePrice);

    public abstract List<Double> getSpecificParameters();

    public double calculateCurrentValue(double currentPrice) {
        return quantity * currentPrice;
    }

    @Override
    public double calculateTaxableGain(double sellingPrice, double unitsSold) {
        double gain = (sellingPrice - purchasePrice) * unitsSold;
        return Math.max(0.0, gain);
    }

    public double calculateSellingTax(double sellingPrice, double unitsSold) {
        return calculateTaxableGain(sellingPrice, unitsSold) * getTaxRate();
    }

    public void addUnits(double units, double unitPrice) {
        double totalQuantity = quantity + units;
        purchasePrice = (quantity * purchasePrice + units * unitPrice) / totalQuantity;
        quantity = totalQuantity;
    }

    public void removeUnits(double units) {
        quantity = Math.max(0.0, quantity - units);
    }

    public boolean holdsAtLeast(double units) {
        return units <= quantity + QUANTITY_TOLERANCE;
    }

    public boolean isSoldOut() {
        return quantity < QUANTITY_TOLERANCE;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }
}
