package com.junia.portfolio.service;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Taxable;

public class AnalyticsService {

    public double calculateUnrealizedGainLoss(Asset asset, double currentPrice) {
        return (currentPrice - asset.getPurchasePrice()) * asset.getQuantity();
    }

    public double calculateReturnOnInvestment(Asset asset, double currentPrice) {
        if (asset.getPurchasePrice() <= 0) {
            return 0.0;
        }
        return ((currentPrice - asset.getPurchasePrice()) / asset.getPurchasePrice()) * 100.0;
    }

    public double estimateSellingTax(Asset asset, double currentPrice, double unitsToSell) {
        if (asset instanceof Taxable taxable) {
            double gain = taxable.calculateTaxableGain(currentPrice, unitsToSell);
            return gain * taxable.getTaxRate();
        }
        return 0.0;
    }

    public double calculateAssetAllocationPercentage(Portfolio portfolio, Asset asset, double currentPrice, double totalPortfolioValue) {
        if (totalPortfolioValue <= 0) {
            return 0.0;
        }
        double assetValue = asset.calculateCurrentValue(currentPrice);
        return (assetValue / totalPortfolioValue) * 100.0;
    }
}
