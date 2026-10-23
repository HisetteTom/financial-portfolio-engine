package com.junia.portfolio.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialFormatterTest {

    @Test
    void shouldFormatBarChart() {
        String bar50 = FinancialFormatter.formatBarChart(50.0, 10);
        assertEquals("[█████     ]", bar50);

        String bar100 = FinancialFormatter.formatBarChart(100.0, 5);
        assertEquals("[█████]", bar100);

        String bar0 = FinancialFormatter.formatBarChart(0.0, 5);
        assertEquals("[     ]", bar0);
    }

    @Test
    void shouldFormatSparkline() {
        List<Double> values = List.of(10.0, 20.0, 50.0, 100.0);
        String sparkline = FinancialFormatter.formatSparkline(values);
        assertEquals(4, sparkline.length());
        assertTrue(sparkline.endsWith("█"));
    }
}
