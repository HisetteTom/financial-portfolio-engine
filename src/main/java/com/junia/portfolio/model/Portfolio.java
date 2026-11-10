package com.junia.portfolio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Portfolio {
    private final List<Asset> assets;
    private double cashBalance;

    public Portfolio() {
        this(0.0);
    }

    public Portfolio(double initialCash) {
        if (initialCash < 0) {
            throw new IllegalArgumentException("Cash balance cannot be negative.");
        }
        this.assets = new ArrayList<>();
        this.cashBalance = initialCash;
    }

    public void addAsset(Asset asset) {
        if (findAsset(asset.getSymbol()).isPresent()) {
            throw new IllegalArgumentException("Asset already in portfolio: " + asset.getSymbol());
        }
        assets.add(asset);
    }

    public void removeAsset(Asset asset) {
        assets.remove(asset);
    }

    public Optional<Asset> findAsset(String symbol) {
        return assets.stream()
                .filter(asset -> asset.getSymbol().equalsIgnoreCase(symbol))
                .findFirst();
    }

    public void clear() {
        assets.clear();
        cashBalance = 0.0;
    }

    public void depositCash(double amount) {
        cashBalance += amount;
    }

    public void withdrawCash(double amount) {
        cashBalance -= amount;
    }

    public List<Asset> getAssets() {
        return Collections.unmodifiableList(assets);
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        if (cashBalance < 0) {
            throw new IllegalArgumentException("Cash balance cannot be negative.");
        }
        this.cashBalance = cashBalance;
    }
}
