package com.junia.portfolio.repository;

import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.utils.AssetCsvParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvPortfolioRepository implements PortfolioRepository {
    private static final String HEADER = "TYPE,SYMBOL,QUANTITY,PURCHASE_PRICE,EXTRA_PARAM,EXTRA_PARAM2";
    private static final String CASH_TYPE = "CASH";
    private static final int COLUMN_COUNT = 6;
    private static final int DECIMALS = 8;

    private final Path initialFile;
    private final Path savedStateFile;
    private final AssetCsvParser assetParser;

    public CsvPortfolioRepository(Path initialFile, Path savedStateFile, AssetCsvParser assetParser) {
        this.initialFile = initialFile;
        this.savedStateFile = savedStateFile;
        this.assetParser = assetParser;
    }

    @Override
    public boolean hasSavedState() {
        return Files.exists(savedStateFile);
    }

    @Override
    public void loadInitialPortfolio(Portfolio portfolio) throws IOException, InvalidDataFileException {
        readInto(portfolio, initialFile);
    }

    @Override
    public void loadSavedState(Portfolio portfolio) throws IOException, InvalidDataFileException {
        readInto(portfolio, savedStateFile);
    }

    @Override
    public void resetToInitial(Portfolio portfolio) throws IOException, InvalidDataFileException {
        loadInitialPortfolio(portfolio);
        Files.deleteIfExists(savedStateFile);
    }

    @Override
    public void save(Portfolio portfolio) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add(toCsvLine(CASH_TYPE, "EUR", portfolio.getCashBalance(), 1.0, List.of()));
        portfolio.getAssets().stream()
                .map(asset -> toCsvLine(asset.getAssetType().name(), asset.getSymbol(),
                        asset.getQuantity(), asset.getPurchasePrice(), asset.getSpecificParameters()))
                .forEach(lines::add);

        if (savedStateFile.getParent() != null) {
            Files.createDirectories(savedStateFile.getParent());
        }
        Files.write(savedStateFile, lines);
    }

    private String toCsvLine(String type, String symbol, double quantity, double price, List<Double> parameters) {
        List<String> columns = new ArrayList<>(List.of(type, symbol, formatNumber(quantity), formatNumber(price)));
        parameters.stream()
                .map(this::formatNumber)
                .forEach(columns::add);
        while (columns.size() < COLUMN_COUNT) {
            columns.add("");
        }
        return String.join(",", columns);
    }

    // BigDecimal always writes a dot, String.format would give 2500,00 on a french system and break the csv
    private String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(DECIMALS, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private void readInto(Portfolio portfolio, Path file) throws IOException, InvalidDataFileException {
        List<String> lines = Files.readAllLines(file);
        // read into a temporary portfolio first so a broken file doesn't wipe the current one
        Portfolio loaded = new Portfolio();

        try {
            lines.stream()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .forEach(line -> readLine(loaded, line));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataFileException("Invalid data in " + file.getFileName() + ": " + e.getMessage());
        }

        portfolio.clear();
        portfolio.setCashBalance(loaded.getCashBalance());
        loaded.getAssets().forEach(portfolio::addAsset);
    }

    private void readLine(Portfolio portfolio, String line) {
        if (line.startsWith(CASH_TYPE + ",")) {
            String[] columns = line.split(",", -1);
            try {
                portfolio.setCashBalance(Double.parseDouble(columns[2].trim()));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid cash line \"" + line + "\"");
            }
        } else {
            portfolio.addAsset(assetParser.parseAssetLine(line));
        }
    }
}
