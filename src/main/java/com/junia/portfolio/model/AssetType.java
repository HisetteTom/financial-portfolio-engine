package com.junia.portfolio.model;

import java.util.List;

public enum AssetType {
    STOCK(1, (symbol, quantity, price, parameters) ->
            new Stock(symbol, quantity, price, parameters.get(0))),
    CRYPTO(1, (symbol, quantity, price, parameters) ->
            new CryptoAsset(symbol, quantity, price, parameters.get(0))),
    BOND(2, (symbol, quantity, price, parameters) ->
            new Bond(symbol, quantity, price, parameters.get(0), parameters.get(1).intValue()));

    private final int parameterCount;
    private final AssetCreator creator;

    AssetType(int parameterCount, AssetCreator creator) {
        this.parameterCount = parameterCount;
        this.creator = creator;
    }

    public Asset create(String symbol, double quantity, double price, List<Double> parameters) {
        if (parameters.size() != parameterCount) {
            throw new IllegalArgumentException(name() + " expects " + parameterCount
                    + " specific parameter(s) but got " + parameters.size());
        }
        return creator.create(symbol, quantity, price, parameters);
    }
}
