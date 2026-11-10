package com.junia.portfolio.model;

import java.util.List;

public interface AssetCreator {
    Asset create(String symbol, double quantity, double price, List<Double> parameters);
}
