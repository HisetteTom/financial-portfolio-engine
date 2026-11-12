package com.junia.portfolio.utils;

import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.model.Asset;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarketCsvParser extends AssetCsvParser {

    @Override
    public List<Asset> parse(Path path) throws IOException, InvalidDataFileException {
        List<Asset> listings = super.parse(path);
        // the market is stored in a map by symbol, a duplicate would silently replace the first line
        Set<String> symbols = new HashSet<>();
        for (Asset listing : listings) {
            if (!symbols.add(listing.getSymbol())) {
                throw new InvalidDataFileException("Duplicate symbol " + listing.getSymbol() + " in " + path.getFileName());
            }
        }
        return listings;
    }

    @Override
    protected Asset parseLine(String line) {
        String[] columns = splitColumns(line, 5);
        // no quantity in the market file, listings stay at 0 and are only used to create new positions
        double price = parseNumber(columns[2], line);
        return createAsset(columns[0], columns[1], 0.0, price, columns[3], columns[4], line);
    }
}
