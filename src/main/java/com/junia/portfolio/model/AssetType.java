package com.junia.portfolio.model;

import java.util.List;

public enum AssetType {
    STOCK(1),
    CRYPTO(1),
    BOND(2);

    private final int parameterCount;

    AssetType(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    // the only place that knows which class goes with which type, so the parser and the services stay generic
    public Asset create(String symbol, double quantity, double price, List<Double> parameters) {
        if (parameters.size() != parameterCount) {
            throw new IllegalArgumentException(name() + " expects " + parameterCount
                    + " specific parameter(s) but got " + parameters.size());
        }

        switch (this) {
            case STOCK:
                return new Stock(symbol, quantity, price, parameters.get(0));
            case CRYPTO:
                return new CryptoAsset(symbol, quantity, price, parameters.get(0));
            default:
                return new Bond(symbol, quantity, price, parameters.get(0), parameters.get(1).intValue());
        }
    }
}
