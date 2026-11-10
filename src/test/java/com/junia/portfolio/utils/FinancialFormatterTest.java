package com.junia.portfolio.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialFormatterTest {

    @Test
    void shouldFormatQuantityWithoutScientificNotation() {
        assertEquals("0.00000001", FinancialFormatter.formatQuantity(0.00000001));
        assertEquals("2", FinancialFormatter.formatQuantity(2.0));
        assertEquals("0.25", FinancialFormatter.formatQuantity(0.25));
    }

    @Test
    void shouldFormatBarChart() {
        assertEquals("[█████     ]", FinancialFormatter.formatBarChart(50.0, 10));
        assertEquals("[█████]", FinancialFormatter.formatBarChart(100.0, 5));
        assertEquals("[     ]", FinancialFormatter.formatBarChart(0.0, 5));
    }

    @Test
    void shouldClampBarChartOutsideZeroToHundred() {
        assertEquals("[█████]", FinancialFormatter.formatBarChart(150.0, 5));
        assertEquals("[     ]", FinancialFormatter.formatBarChart(-20.0, 5));
    }

    @Test
    void shouldScaleSparklineFromLowestToHighestValue() {
        String sparkline = FinancialFormatter.formatSparkline(List.of(10.0, 20.0, 50.0, 100.0));

        assertEquals(4, sparkline.length());
        assertEquals(' ', sparkline.charAt(0));
        assertEquals('█', sparkline.charAt(3));
    }

    @Test
    void shouldHandleNegativeValuesInSparkline() {
        String sparkline = FinancialFormatter.formatSparkline(List.of(-50.0, -20.0, -10.0));

        assertEquals(' ', sparkline.charAt(0));
        assertEquals('█', sparkline.charAt(2));
    }

    @Test
    void shouldDrawFlatSparklineWhenValuesAreEqual() {
        assertEquals("▄▄▄", FinancialFormatter.formatSparkline(List.of(5.0, 5.0, 5.0)));
        assertEquals("", FinancialFormatter.formatSparkline(List.of()));
    }
}
