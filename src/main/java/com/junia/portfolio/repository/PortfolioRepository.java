package com.junia.portfolio.repository;

import com.junia.portfolio.exception.InvalidDataFileException;
import com.junia.portfolio.model.Portfolio;

import java.io.IOException;

public interface PortfolioRepository {
    boolean hasSavedState();

    void loadInitialPortfolio(Portfolio portfolio) throws IOException, InvalidDataFileException;

    void loadSavedState(Portfolio portfolio) throws IOException, InvalidDataFileException;

    void resetToInitial(Portfolio portfolio) throws IOException, InvalidDataFileException;

    void save(Portfolio portfolio) throws IOException;
}
