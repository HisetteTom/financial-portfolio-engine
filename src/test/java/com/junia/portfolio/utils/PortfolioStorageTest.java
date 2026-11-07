package com.junia.portfolio.utils;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioStorageTest {
    private Portfolio portfolio;
    private AssetCsvParser parser;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(2500.0);
        portfolio.addAsset(new Stock("AAPL", 10.0, 150.0, 0.01));
        portfolio.addAsset(new CryptoAsset("BTC", 0.5, 60000.0, 12.0));
        portfolio.addAsset(new Bond("US10Y", 5.0, 1000.0, 0.04, 2034));
        parser = new AssetCsvParser();
    }

    @Test
    void shouldSaveAndReloadPortfolioSuccessfully(@TempDir Path tempDir) throws IOException {
        Path savePath = tempDir.resolve("saved_portfolio.csv");

        PortfolioStorage.save(portfolio, savePath);
        assertTrue(savePath.toFile().exists());

        Portfolio reloaded = new Portfolio(0.0);
        PortfolioStorage.load(reloaded, savePath, parser);

        assertEquals(2500.0, reloaded.getCashBalance(), 0.001);
        assertEquals(3, reloaded.getAssets().size());

        Asset reloadedStock = reloaded.getAssets().get(0);
        assertEquals("AAPL", reloadedStock.getSymbol());
        assertEquals(10.0, reloadedStock.getQuantity(), 0.001);
    }
}
