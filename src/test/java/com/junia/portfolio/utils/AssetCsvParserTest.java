package com.junia.portfolio.utils;

import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AssetCsvParserTest {
    private AssetCsvParser parser;
    private Path sampleCsvPath;

    @BeforeEach
    void setUp() {
        parser = new AssetCsvParser();
        sampleCsvPath = Paths.get("src/main/resources/assets.csv");
    }

    @Test
    void shouldParseAssetsFromCsvFile() {
        List<Asset> assets = parser.parse(sampleCsvPath);

        assertNotNull(assets);
        assertEquals(6, assets.size());

        Asset first = assets.get(0);
        assertInstanceOf(Stock.class, first);
        assertEquals("AAPL", first.getSymbol());
        assertEquals(15.0, first.getQuantity(), 0.001);

        Asset third = assets.get(2);
        assertInstanceOf(CryptoAsset.class, third);
        assertEquals("BTC", third.getSymbol());

        Asset fifth = assets.get(4);
        assertInstanceOf(Bond.class, fifth);
        assertEquals("US10Y", fifth.getSymbol());
        Bond bond = (Bond) fifth;
        assertEquals(2034, bond.getMaturityYear());
    }
}
