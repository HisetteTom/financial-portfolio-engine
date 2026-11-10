package com.junia.portfolio.model;

public class PositionSummary {
    private final String symbol;
    private final AssetType assetType;
    private final double quantity;
    private final double purchasePrice;
    private final double marketPrice;
    private final double currentValue;
    private final double gainLoss;
    private final double returnOnInvestment;
    private final double allocation;
    private final double annualIncome;

    public PositionSummary(Asset asset, double marketPrice, double gainLoss, double returnOnInvestment, double allocation) {
        this.symbol = asset.getSymbol();
        this.assetType = asset.getAssetType();
        this.quantity = asset.getQuantity();
        this.purchasePrice = asset.getPurchasePrice();
        this.marketPrice = marketPrice;
        this.currentValue = asset.calculateCurrentValue(marketPrice);
        this.gainLoss = gainLoss;
        this.returnOnInvestment = returnOnInvestment;
        this.allocation = allocation;
        this.annualIncome = asset.calculateAnnualIncome(marketPrice);
    }

    public String getSymbol() {
        return symbol;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public double getMarketPrice() {
        return marketPrice;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public double getGainLoss() {
        return gainLoss;
    }

    public double getReturnOnInvestment() {
        return returnOnInvestment;
    }

    public double getAllocation() {
        return allocation;
    }

    public double getAnnualIncome() {
        return annualIncome;
    }
}
