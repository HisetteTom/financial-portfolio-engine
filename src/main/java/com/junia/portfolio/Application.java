package com.junia.portfolio;

import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.repository.CsvPortfolioRepository;
import com.junia.portfolio.repository.PortfolioRepository;
import com.junia.portfolio.service.AnalyticsService;
import com.junia.portfolio.service.MarketDataService;
import com.junia.portfolio.service.PortfolioService;
import com.junia.portfolio.ui.console.ConsoleMenu;
import com.junia.portfolio.ui.web.PortfolioWebController;
import com.junia.portfolio.utils.AssetCsvParser;
import com.junia.portfolio.utils.MarketCsvParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.Scanner;

public class Application {
    private static final Path INITIAL_PORTFOLIO_FILE = Path.of("src/main/resources/assets.csv");
    private static final Path MARKET_FILE = Path.of("src/main/resources/market.csv");
    private static final Path SAVED_STATE_FILE = Path.of("data/portfolio_state.csv");
    private static final int WEB_PORT = 7070;

    public static void main(String[] args) {
        try {
            MarketDataService marketDataService = new MarketDataService(new MarketCsvParser().parse(MARKET_FILE), new Random());
            PortfolioRepository repository = new CsvPortfolioRepository(INITIAL_PORTFOLIO_FILE, SAVED_STATE_FILE, new AssetCsvParser());
            Portfolio portfolio = loadPortfolio(repository);

            PortfolioService portfolioService = new PortfolioService(portfolio, marketDataService);
            AnalyticsService analyticsService = new AnalyticsService(marketDataService);

            if (args.length > 0 && args[0].equalsIgnoreCase("--web")) {
                new PortfolioWebController(portfolioService, marketDataService, analyticsService, repository).start(WEB_PORT);
                System.out.println("Web dashboard running at http://localhost:" + WEB_PORT);
            } else {
                new ConsoleMenu(portfolioService, marketDataService, analyticsService, repository, new Scanner(System.in)).run();
            }
        } catch (IOException e) {
            System.out.println("Could not read data file: " + e.getMessage());
            System.out.println("Run the application from the project folder.");
        } catch (InvalidDataFileException e) {
            System.out.println("Data file error: " + e.getMessage());
        }
    }

    private static Portfolio loadPortfolio(PortfolioRepository repository) throws IOException, InvalidDataFileException {
        Portfolio portfolio = new Portfolio();
        if (repository.hasSavedState()) {
            try {
                repository.loadSavedState(portfolio);
                System.out.println("Saved portfolio restored from " + SAVED_STATE_FILE + ".");
                return portfolio;
            } catch (InvalidDataFileException e) {
                System.out.println("Saved state is invalid (" + e.getMessage() + "), loading the initial portfolio.");
            }
        }
        repository.loadInitialPortfolio(portfolio);
        return portfolio;
    }
}
