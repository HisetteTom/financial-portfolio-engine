package com.junia.portfolio.service;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Order;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.utils.AssetCsvParser;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PortfolioService {
    private final Portfolio portfolio;
    private final AssetCsvParser csvParser;

    public PortfolioService(Portfolio portfolio, AssetCsvParser csvParser) {
        this.portfolio = portfolio;
        this.csvParser = csvParser;
    }

    public void loadAssetsFromCsv(Path csvPath) {
        List<Asset> assets = csvParser.parse(csvPath);
        for (Asset asset : assets) {
            portfolio.addAsset(asset);
        }
    }

    public double calculateTotalValue(Map<String, Double> marketPrices) {
        double totalAssetsValue = portfolio.getAssets().stream()
                .mapToDouble(asset -> {
                    double currentPrice = marketPrices.getOrDefault(asset.getSymbol(), asset.getPurchasePrice());
                    return asset.calculateCurrentValue(currentPrice);
                })
                .sum();
        return totalAssetsValue + portfolio.getCashBalance();
    }

    public Optional<Asset> findAssetBySymbol(String symbol) {
        return portfolio.getAssets().stream()
                .filter(asset -> asset.getSymbol().equalsIgnoreCase(symbol))
                .findFirst();
    }

    public void executeOrder(Order order) throws InsufficientFundsException, AssetNotFoundException, InvalidTransactionException {
        if (order.getQuantity() <= 0 || order.getPrice() <= 0) {
            throw new InvalidTransactionException("Order quantity and price must be strictly positive.");
        }

        if (order.getType() == OrderType.BUY) {
            executeBuy(order);
        } else if (order.getType() == OrderType.SELL) {
            executeSell(order);
        }
    }

    private void executeBuy(Order order) throws InsufficientFundsException {
        double totalCost = order.getTotalValue();
        if (portfolio.getCashBalance() < totalCost) {
            throw new InsufficientFundsException("Insufficient funds to buy " + order.getQuantity() + " of " + order.getSymbol());
        }

        portfolio.setCashBalance(portfolio.getCashBalance() - totalCost);

        Optional<Asset> existing = findAssetBySymbol(order.getSymbol());
        if (existing.isPresent()) {
            Asset asset = existing.get();
            double totalQuantity = asset.getQuantity() + order.getQuantity();
            double weightedPrice = ((asset.getQuantity() * asset.getPurchasePrice()) + totalCost) / totalQuantity;
            asset.setQuantity(totalQuantity);
            asset.setPurchasePrice(weightedPrice);
        }
    }

    private void executeSell(Order order) throws AssetNotFoundException, InvalidTransactionException {
        Asset asset = findAssetBySymbol(order.getSymbol())
                .orElseThrow(() -> new AssetNotFoundException("Asset not found in portfolio: " + order.getSymbol()));

        if (asset.getQuantity() < order.getQuantity()) {
            throw new InvalidTransactionException("Cannot sell more quantity than currently owned.");
        }

        double proceeds = order.getTotalValue();
        asset.setQuantity(asset.getQuantity() - order.getQuantity());
        portfolio.setCashBalance(portfolio.getCashBalance() + proceeds);

        if (asset.getQuantity() == 0) {
            portfolio.getAssets().remove(asset);
        }
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }
}
