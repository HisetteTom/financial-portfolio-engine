package com.junia.portfolio.repository;

import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.model.Bond;
import com.junia.portfolio.model.CryptoAsset;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.Stock;
import com.junia.portfolio.utils.AssetCsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvPortfolioRepositoryTest {
    private static final Path INITIAL_FILE = Path.of("src/main/resources/assets.csv");

    @TempDir
    Path tempDir;

    private Path savedStateFile;
    private CsvPortfolioRepository repository;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        savedStateFile = tempDir.resolve("data/portfolio_state.csv");
        repository = new CsvPortfolioRepository(INITIAL_FILE, savedStateFile, new AssetCsvParser());

        portfolio = new Portfolio(2500.0);
        portfolio.addAsset(new Stock("AAPL", 10.0, 152.3456, 0.01));
        portfolio.addAsset(new CryptoAsset("BTC", 0.12345678, 60000.0, 12.0));
        portfolio.addAsset(new Bond("US10Y", 5.0, 1000.0, 0.04, 2034));
    }

    @Test
    void shouldLoadInitialPortfolioWithCashFromFile() throws Exception {
        Portfolio loaded = new Portfolio();

        repository.loadInitialPortfolio(loaded);

        assertEquals(5000.0, loaded.getCashBalance(), 0.001);
        assertEquals(6, loaded.getAssets().size());
        assertFalse(repository.hasSavedState());
    }

    @Test
    void shouldSaveAndReloadEveryField() throws Exception {
        repository.save(portfolio);
        Portfolio reloaded = new Portfolio();
        repository.loadSavedState(reloaded);

        assertTrue(repository.hasSavedState());
        assertEquals(2500.0, reloaded.getCashBalance(), 0.001);
        assertEquals(3, reloaded.getAssets().size());

        Stock stock = assertInstanceOf(Stock.class, reloaded.findAsset("AAPL").orElseThrow());
        assertEquals(152.3456, stock.getPurchasePrice(), 0.0001);
        assertEquals(0.01, stock.getAnnualDividendRate(), 0.0001);

        CryptoAsset crypto = assertInstanceOf(CryptoAsset.class, reloaded.findAsset("BTC").orElseThrow());
        assertEquals(0.12345678, crypto.getQuantity(), 0.00000001);
        assertEquals(12.0, crypto.getNetworkGasFee(), 0.001);

        Bond bond = assertInstanceOf(Bond.class, reloaded.findAsset("US10Y").orElseThrow());
        assertEquals(2034, bond.getMaturityYear());
    }

    @Test
    void shouldWriteDotDecimalsEvenWithFrenchLocale() throws Exception {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            repository.save(portfolio);
            Portfolio reloaded = new Portfolio();
            repository.loadSavedState(reloaded);

            assertEquals(2500.0, reloaded.getCashBalance(), 0.001);
            assertEquals(152.3456, reloaded.findAsset("AAPL").orElseThrow().getPurchasePrice(), 0.0001);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void shouldResetToInitialPortfolioAndDeleteSavedState() throws Exception {
        repository.save(portfolio);

        repository.resetToInitial(portfolio);

        assertFalse(repository.hasSavedState());
        assertEquals(5000.0, portfolio.getCashBalance(), 0.001);
        assertEquals(6, portfolio.getAssets().size());
    }

    @Test
    void shouldKeepPortfolioUnchangedWhenSavedStateIsCorrupted() throws Exception {
        Files.createDirectories(savedStateFile.getParent());
        Files.write(savedStateFile, List.of(
                "TYPE,SYMBOL,QUANTITY,PURCHASE_PRICE,EXTRA_PARAM,EXTRA_PARAM2",
                "CASH,EUR,100.00,1.0,,",
                "STOCK,AAPL,ten,150.00,0.01,"));

        assertThrows(InvalidDataFileException.class, () -> repository.loadSavedState(portfolio));
        assertEquals(2500.0, portfolio.getCashBalance(), 0.001);
        assertEquals(3, portfolio.getAssets().size());
    }

    @Test
    void shouldWriteReadableCsvLines() throws Exception {
        repository.save(portfolio);

        List<String> lines = Files.readAllLines(savedStateFile);
        assertEquals("CASH,EUR,2500,1,,", lines.get(1));
        assertEquals("STOCK,AAPL,10,152.3456,0.01,", lines.get(2));
        assertEquals("CRYPTO,BTC,0.12345678,60000,12,", lines.get(3));
        assertEquals("BOND,US10Y,5,1000,0.04,2034", lines.get(4));
    }

    @Test
    void shouldRefuseFileWithDuplicateSymbol() throws Exception {
        Files.createDirectories(savedStateFile.getParent());
        Files.write(savedStateFile, List.of(
                "TYPE,SYMBOL,QUANTITY,PURCHASE_PRICE,EXTRA_PARAM,EXTRA_PARAM2",
                "STOCK,AAPL,1.0,150.00,0.01,",
                "STOCK,AAPL,2.0,160.00,0.01,"));

        assertThrows(InvalidDataFileException.class, () -> repository.loadSavedState(new Portfolio()));
    }
}
