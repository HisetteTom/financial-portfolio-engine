package com.junia.portfolio.service;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.PositionSummary;

import java.util.List;

public class AnalyticsService {
    private final MarketDataService marketDataService;

    public AnalyticsService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public double calculateTotalValue(Portfolio portfolio) {
        double assetsValue = portfolio.getAssets().stream()
                .mapToDouble(asset -> asset.calculateCurrentValue(marketDataService.getCurrentPrice(asset)))
                .sum();
        return assetsValue + portfolio.getCashBalance();
    }

    public double calculateTotalCostBasis(Portfolio portfolio) {
        return portfolio.getAssets().stream()
                .mapToDouble(asset -> asset.getQuantity() * asset.getPurchasePrice())
                .sum();
    }

    public double calculateUnrealizedGainLoss(Asset asset) {
        return (marketDataService.getCurrentPrice(asset) - asset.getPurchasePrice()) * asset.getQuantity();
    }

    public double calculateTotalUnrealizedGainLoss(Portfolio portfolio) {
        return portfolio.getAssets().stream()
                .mapToDouble(asset -> calculateUnrealizedGainLoss(asset))
                .sum();
    }

    public double calculateReturnOnInvestment(Asset asset) {
        double currentPrice = marketDataService.getCurrentPrice(asset);
        return ((currentPrice - asset.getPurchasePrice()) / asset.getPurchasePrice()) * 100.0;
    }

    public double calculateProjectedAnnualIncome(Portfolio portfolio) {
        return portfolio.getAssets().stream()
                .mapToDouble(asset -> asset.calculateAnnualIncome(marketDataService.getCurrentPrice(asset)))
                .sum();
    }

    public double estimateSellingTax(Asset asset, double unitsToSell) {
        return asset.calculateSellingTax(marketDataService.getCurrentPrice(asset), unitsToSell);
    }

    public double calculateAllocationPercentage(Asset asset, double totalPortfolioValue) {
        if (totalPortfolioValue <= 0) {
            return 0.0;
        }
        double assetValue = asset.calculateCurrentValue(marketDataService.getCurrentPrice(asset));
        return (assetValue / totalPortfolioValue) * 100.0;
    }

    public List<PositionSummary> summarizePositions(Portfolio portfolio) {
        double totalValue = calculateTotalValue(portfolio);
        return portfolio.getAssets().stream()
                .map(asset -> new PositionSummary(
                        asset,
                        marketDataService.getCurrentPrice(asset),
                        calculateUnrealizedGainLoss(asset),
                        calculateReturnOnInvestment(asset),
                        calculateAllocationPercentage(asset, totalValue)))
                .toList();
    }
}
