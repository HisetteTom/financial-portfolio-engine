package com.junia.portfolio.model;

import java.util.ArrayList;
import java.util.List;

public class Portfolio {
    private List<Asset> assets;
    private double cashBalance;

    public Portfolio() {
        this.assets = new ArrayList<>();
        this.cashBalance = 0.0;
    }

    public Portfolio(double initialCash) {
        this.assets = new ArrayList<>();
        this.cashBalance = initialCash;
    }

    public void addAsset(Asset asset) {
        assets.add(asset);
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }
}
