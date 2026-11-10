package com.junia.portfolio.utils;

import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.CryptoAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketCsvParserTest {
    private final MarketCsvParser parser = new MarketCsvParser();

    @Test
    void shouldLoadProjectMarketFile() throws Exception {
        List<Asset> listings = parser.parse(Path.of("src/main/resources/market.csv"));

        assertEquals(10, listings.size());
        assertTrue(listings.stream().allMatch(listing -> listing.getQuantity() == 0.0));
    }

    @Test
    void shouldUseMarketPriceAsListingPrice(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("market.csv");
        Files.write(file, List.of("TYPE,SYMBOL,PRICE,EXTRA_PARAM,EXTRA_PARAM2", "CRYPTO,SOL,145.00,0.50,"));

        CryptoAsset solana = assertInstanceOf(CryptoAsset.class, parser.parse(file).get(0));

        assertEquals("SOL", solana.getSymbol());
        assertEquals(145.0, solana.getPurchasePrice(), 0.001);
        assertEquals(0.5, solana.getNetworkGasFee(), 0.001);
    }

    @Test
    void shouldRefuseLineWithPortfolioLayout(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("market.csv");
        Files.write(file, List.of("TYPE,SYMBOL,PRICE,EXTRA_PARAM,EXTRA_PARAM2", "STOCK,AAPL,15.0,175.50,0.005,"));

        assertThrows(InvalidDataFileException.class, () -> parser.parse(file));
    }
}
