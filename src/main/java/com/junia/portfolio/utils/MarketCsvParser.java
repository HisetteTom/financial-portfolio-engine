package com.junia.portfolio.utils;

import com.junia.portfolio.model.Asset;

public class MarketCsvParser extends AssetCsvParser {

    @Override
    protected Asset parseLine(String line) {
        String[] columns = splitColumns(line, 5);
        // no quantity in the market file, listings stay at 0 and are only used to create new positions
        double price = parseNumber(columns[2], line);
        return createAsset(columns[0], columns[1], 0.0, price, columns[3], columns[4], line);
    }
}
