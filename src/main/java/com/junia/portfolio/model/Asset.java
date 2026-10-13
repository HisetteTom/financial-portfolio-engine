package com.junia.portfolio.model;

public abstract class Asset {
    private String symbol;
    private double quantity;
    private double purchasePrice;

    public Asset(String symbol, double quantity, double purchasePrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
    }

    public abstract double calculateCurrentValue(double currentPrice);

    public String getSymbol() {
        return symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
}
