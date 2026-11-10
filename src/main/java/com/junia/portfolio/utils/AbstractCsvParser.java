package com.junia.portfolio.utils;

import com.junia.portfolio.exception.InvalidDataFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractCsvParser<T> {

    public List<T> parse(Path path) throws IOException, InvalidDataFileException {
        try (var lines = Files.lines(path)) {
            return lines
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(this::parseLine)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new InvalidDataFileException("Invalid data in " + path.getFileName() + ": " + e.getMessage());
        }
    }

    protected abstract T parseLine(String line);
}
