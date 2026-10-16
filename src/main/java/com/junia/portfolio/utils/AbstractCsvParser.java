package com.junia.portfolio.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractCsvParser<T> {

    public List<T> parse(Path path) {
        try (var lines = Files.lines(path)) {
            return lines
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(this::parseLine)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + path, e);
        }
    }

    protected abstract T parseLine(String line);
}
