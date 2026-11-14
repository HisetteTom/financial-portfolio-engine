package com.junia.portfolio.utils;

import com.junia.portfolio.exception.InvalidDataFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractCsvParser<T> {

    public List<T> parse(Path path) throws IOException, InvalidDataFileException {
        try (Stream<String> lines = Files.lines(path)) {
            return lines
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(line -> parseLine(line))
                    .toList();
        } catch (IllegalArgumentException e) {
            // parseLine runs inside a stream, so it can't throw a checked exception: we convert it here
            throw new InvalidDataFileException("Invalid data in " + path.getFileName() + ": " + e.getMessage());
        }
    }

    protected abstract T parseLine(String line);
}
