package com.junia.portfolio.utils;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.AssetType;

import java.util.ArrayList;
import java.util.List;

public class AssetCsvParser extends AbstractCsvParser<Asset> {

    @Override
    protected Asset parseLine(String line) {
        String[] columns = splitColumns(line, 6);
        double quantity = parseNumber(columns[2], line);
        double purchasePrice = parseNumber(columns[3], line);
        return createAsset(columns[0], columns[1], quantity, purchasePrice, columns[4], columns[5], line);
    }

    public Asset parseAssetLine(String line) {
        return parseLine(line);
    }

    protected String[] splitColumns(String line, int expectedColumns) {
        // -1 keeps the empty columns at the end of the line, like the missing EXTRA_PARAM2 of a stock
        String[] columns = line.split(",", -1);
        if (columns.length != expectedColumns) {
            throw new IllegalArgumentException("expected " + expectedColumns + " columns in line \"" + line + "\"");
        }
        return columns;
    }

    protected double parseNumber(String value, String line) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid number \"" + value + "\" in line \"" + line + "\"");
        }
    }

    protected Asset createAsset(String type, String symbol, double quantity, double price,
                                String extraParam, String secondExtraParam, String line) {
        List<Double> parameters = new ArrayList<>();
        if (!extraParam.isBlank()) {
            parameters.add(parseNumber(extraParam, line));
        }
        if (!secondExtraParam.isBlank()) {
            parameters.add(parseNumber(secondExtraParam, line));
        }
        try {
            return parseType(type, line).create(symbol, quantity, price, parameters);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + " in line \"" + line + "\"");
        }
    }

    private AssetType parseType(String type, String line) {
        try {
            return AssetType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown asset type \"" + type + "\"");
        }
    }
}
