package com.junia.portfolio.utils;

import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetCsvParserTest {
    private static final String HEADER = "TYPE,SYMBOL,QUANTITY,PURCHASE_PRICE,EXTRA_PARAM,EXTRA_PARAM2";

    @TempDir
    Path tempDir;

    private AssetCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new AssetCsvParser();
    }

    private Path writeCsv(String... lines) throws IOException {
        Path file = tempDir.resolve("assets.csv");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    void shouldCreateRightSubclassForEachLine() throws Exception {
        Path file = writeCsv(HEADER,
                "STOCK,AAPL,15.0,175.50,0.005,",
                "CRYPTO,BTC,0.45,61200.00,15.00,",
                "BOND,US10Y,10.0,1000.00,0.045,2034");

        List<Asset> assets = parser.parse(file);

        assertEquals(3, assets.size());
        assertInstanceOf(Stock.class, assets.get(0));
        assertInstanceOf(CryptoAsset.class, assets.get(1));
        Bond bond = assertInstanceOf(Bond.class, assets.get(2));
        assertEquals(2034, bond.getMaturityYear());
        assertEquals(0.045, bond.getCouponRate(), 0.0001);
    }

    @Test
    void shouldIgnoreBlankLines() throws Exception {
        Path file = writeCsv(HEADER, "", "STOCK,AAPL,15.0,175.50,0.005,", "   ");

        assertEquals(1, parser.parse(file).size());
    }

    @Test
    void shouldReturnEmptyListWhenOnlyHeaderIsPresent() throws Exception {
        Path file = writeCsv(HEADER);

        assertTrue(parser.parse(file).isEmpty());
    }

    @Test
    void shouldThrowExceptionForInvalidNumber() throws IOException {
        Path file = writeCsv(HEADER, "STOCK,AAPL,abc,175.50,0.005,");

        InvalidDataFileException exception = assertThrows(InvalidDataFileException.class, () -> parser.parse(file));
        assertTrue(exception.getMessage().contains("abc"));
    }

    @Test
    void shouldThrowExceptionForUnknownAssetType() throws IOException {
        Path file = writeCsv(HEADER, "GOLD,XAU,1.0,2000.00,0.0,");

        assertThrows(InvalidDataFileException.class, () -> parser.parse(file));
    }

    @Test
    void shouldThrowExceptionWhenBondMaturityIsMissing() throws IOException {
        Path file = writeCsv(HEADER, "BOND,US10Y,10.0,1000.00,0.045,");

        assertThrows(InvalidDataFileException.class, () -> parser.parse(file));
    }

    @Test
    void shouldThrowExceptionForMissingColumns() throws IOException {
        Path file = writeCsv(HEADER, "STOCK,AAPL,15.0");

        assertThrows(InvalidDataFileException.class, () -> parser.parse(file));
    }

    @Test
    void shouldThrowExceptionForNegativeQuantity() throws IOException {
        Path file = writeCsv(HEADER, "STOCK,AAPL,-3.0,175.50,0.005,");

        assertThrows(InvalidDataFileException.class, () -> parser.parse(file));
    }

    @Test
    void shouldThrowIOExceptionWhenFileDoesNotExist() {
        Path missing = tempDir.resolve("missing.csv");

        assertThrows(IOException.class, () -> parser.parse(missing));
    }
}
