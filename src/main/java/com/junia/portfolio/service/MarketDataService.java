package com.junia.portfolio.service;

import com.junia.portfolio.model.Asset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class MarketDataService {
    private static final double MAX_DAILY_VARIATION = 0.04;
    private static final double MINIMUM_PRICE = 0.01;

    private final Map<String, Asset> listings;
    private final Map<String, Double> currentPrices;
    private final Map<String, List<Double>> priceHistory;
    private final Random random;

    public MarketDataService(List<Asset> listedAssets, Random random) {
        // LinkedHashMap keeps the order of the market file for the display
        this.listings = new LinkedHashMap<>();
        this.currentPrices = new LinkedHashMap<>();
        this.priceHistory = new LinkedHashMap<>();
        this.random = random;

        for (Asset asset : listedAssets) {
            listings.put(asset.getSymbol(), asset);
            currentPrices.put(asset.getSymbol(), asset.getPurchasePrice());
            List<Double> history = new ArrayList<>();
            history.add(asset.getPurchasePrice());
            priceHistory.put(asset.getSymbol(), history);
        }
    }

    public Optional<Asset> findListing(String symbol) {
        return Optional.ofNullable(listings.get(symbol.toUpperCase()));
    }

    public Optional<Double> findPrice(String symbol) {
        return Optional.ofNullable(currentPrices.get(symbol.toUpperCase()));
    }

    public double getCurrentPrice(Asset asset) {
        if (currentPrices.containsKey(asset.getSymbol())) {
            return currentPrices.get(asset.getSymbol());
        }
        // an asset that is not on the market keeps its purchase price
        return asset.getPurchasePrice();
    }

    public void setPrice(String symbol, double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Market price must be positive.");
        }
        String key = symbol.toUpperCase();
        currentPrices.put(key, price);
        addToHistory(key, price);
    }

    private void addToHistory(String symbol, double price) {
        if (!priceHistory.containsKey(symbol)) {
            priceHistory.put(symbol, new ArrayList<>());
        }
        priceHistory.get(symbol).add(price);
    }

    public Map<String, Double> getAllPrices() {
        return new LinkedHashMap<>(currentPrices);
    }

    public List<Asset> getListings() {
        return new ArrayList<>(listings.values());
    }

    public List<Double> getPriceHistory(String symbol) {
        List<Double> history = priceHistory.get(symbol.toUpperCase());
        if (history == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history);
    }

    public void simulateMarketDay() {
        for (String symbol : currentPrices.keySet()) {
            double changePercent = (random.nextDouble() * 2 * MAX_DAILY_VARIATION) - MAX_DAILY_VARIATION;
            double newPrice = Math.round(currentPrices.get(symbol) * (1.0 + changePercent) * 100.0) / 100.0;
            newPrice = Math.max(MINIMUM_PRICE, newPrice);

            currentPrices.put(symbol, newPrice);
            addToHistory(symbol, newPrice);
        }
    }
}
