package com.junia.portfolio.utils;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Stock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PortfolioStorage {

    public static void save(Portfolio portfolio, Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("TYPE,SYMBOL,QUANTITY,PURCHASE_PRICE,EXTRA_PARAM,EXTRA_PARAM2");
        lines.add(String.format("CASH,EUR,%.2f,1.0,,", portfolio.getCashBalance()));

        for (Asset asset : portfolio.getAssets()) {
            if (asset instanceof Stock stock) {
                lines.add(String.format("STOCK,%s,%.4f,%.2f,%.4f,",
                        stock.getSymbol(), stock.getQuantity(), stock.getPurchasePrice(), stock.getAnnualDividendRate()));
            } else if (asset instanceof CryptoAsset crypto) {
                lines.add(String.format("CRYPTO,%s,%.8f,%.2f,%.2f,",
                        crypto.getSymbol(), crypto.getQuantity(), crypto.getPurchasePrice(), crypto.getNetworkGasFee()));
            } else if (asset instanceof Bond bond) {
                lines.add(String.format("BOND,%s,%.4f,%.2f,%.4f,%d",
                        bond.getSymbol(), bond.getQuantity(), bond.getPurchasePrice(), bond.getCouponRate(), bond.getMaturityYear()));
            }
        }

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, lines);
    }

    public static void load(Portfolio portfolio, Path path, AssetCsvParser parser) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        portfolio.getAssets().clear();
        List<String> lines = Files.readAllLines(path);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("CASH")) {
                String[] cols = line.split(",", -1);
                portfolio.setCashBalance(Double.parseDouble(cols[2].trim()));
            } else {
                Asset asset = parser.parseLine(line);
                portfolio.addAsset(asset);
            }
        }
    }
}
