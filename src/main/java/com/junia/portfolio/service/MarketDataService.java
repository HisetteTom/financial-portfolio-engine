package com.junia.portfolio.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MarketDataService {
    private final Map<String, Double> currentPrices;
    private final Map<String, List<Double>> priceHistory;
    private final Random random;

    public MarketDataService() {
        this.currentPrices = new HashMap<>();
        this.priceHistory = new HashMap<>();
        this.random = new Random();
    }

    public MarketDataService(Map<String, Double> initialPrices) {
        this.currentPrices = new HashMap<>(initialPrices);
        this.priceHistory = new HashMap<>();
        this.random = new Random();

        for (Map.Entry<String, Double> entry : initialPrices.entrySet()) {
            List<Double> history = new ArrayList<>();
            history.add(entry.getValue());
            priceHistory.put(entry.getKey(), history);
        }
    }

    public void setPrice(String symbol, double price) {
        currentPrices.put(symbol.toUpperCase(), price);
        priceHistory.computeIfAbsent(symbol.toUpperCase(), k -> new ArrayList<>()).add(price);
    }

    public double getPrice(String symbol) {
        return currentPrices.getOrDefault(symbol.toUpperCase(), 0.0);
    }

    public Map<String, Double> getAllPrices() {
        return Collections.unmodifiableMap(currentPrices);
    }

    public List<Double> getPriceHistory(String symbol) {
        return priceHistory.getOrDefault(symbol.toUpperCase(), Collections.emptyList());
    }

    public void simulateMarketDay() {
        for (Map.Entry<String, Double> entry : currentPrices.entrySet()) {
            String symbol = entry.getKey();
            double oldPrice = entry.getValue();

            double changePercent = (random.nextDouble() * 0.08) - 0.04;
            double newPrice = Math.round(oldPrice * (1.0 + changePercent) * 100.0) / 100.0;
            if (newPrice <= 0.01) {
                newPrice = 0.01;
            }

            currentPrices.put(symbol, newPrice);
            priceHistory.computeIfAbsent(symbol, k -> new ArrayList<>()).add(newPrice);
        }
    }
}
