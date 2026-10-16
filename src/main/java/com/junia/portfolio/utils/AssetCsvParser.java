package com.junia.portfolio.utils;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Stock;

public class AssetCsvParser extends AbstractCsvParser<Asset> {

    @Override
    protected Asset parseLine(String line) {
        String[] columns = line.split(",", -1);

        String type = columns[0].trim();
        String symbol = columns[1].trim();
        double quantity = Double.parseDouble(columns[2].trim());
        double purchasePrice = Double.parseDouble(columns[3].trim());

        return switch (type.toUpperCase()) {
            case "STOCK" -> {
                double dividendRate = Double.parseDouble(columns[4].trim());
                yield new Stock(symbol, quantity, purchasePrice, dividendRate);
            }
            case "CRYPTO" -> {
                double gasFee = Double.parseDouble(columns[4].trim());
                yield new CryptoAsset(symbol, quantity, purchasePrice, gasFee);
            }
            case "BOND" -> {
                double couponRate = Double.parseDouble(columns[4].trim());
                int maturityYear = Integer.parseInt(columns[5].trim());
                yield new Bond(symbol, quantity, purchasePrice, couponRate, maturityYear);
            }
            default -> throw new IllegalArgumentException("Unknown asset type: " + type);
        };
    }
}
