package com.junia.portfolio.service;

import com.junia.portfolio.model.Asset;

import java.util.ArrayList;
import java.util.Collections;
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
        return currentPrices.getOrDefault(asset.getSymbol(), asset.getPurchasePrice());
    }

    public void setPrice(String symbol, double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Market price must be positive.");
        }
        String key = symbol.toUpperCase();
        currentPrices.put(key, price);
        priceHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(price);
    }

    public Map<String, Double> getAllPrices() {
        return Collections.unmodifiableMap(currentPrices);
    }

    public List<Asset> getListings() {
        return List.copyOf(listings.values());
    }

    public List<Double> getPriceHistory(String symbol) {
        return Collections.unmodifiableList(priceHistory.getOrDefault(symbol.toUpperCase(), List.of()));
    }

    public void simulateMarketDay() {
        for (Map.Entry<String, Double> entry : currentPrices.entrySet()) {
            double changePercent = (random.nextDouble() * 2 * MAX_DAILY_VARIATION) - MAX_DAILY_VARIATION;
            double newPrice = Math.round(entry.getValue() * (1.0 + changePercent) * 100.0) / 100.0;
            newPrice = Math.max(MINIMUM_PRICE, newPrice);

            entry.setValue(newPrice);
            priceHistory.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(newPrice);
        }
    }
}
