package com.junia.portfolio.utils;

import java.math.BigDecimal;
import java.util.List;

public class FinancialFormatter {
    private static final char[] SPARKLINE_LEVELS = {' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'};

    public static String formatBarChart(double percentage, int totalWidth) {
        double clamped = Math.max(0.0, Math.min(100.0, percentage));
        int filledCount = (int) Math.round((clamped / 100.0) * totalWidth);
        int emptyCount = totalWidth - filledCount;

        return "[" + "█".repeat(filledCount) + " ".repeat(emptyCount) + "]";
    }

    public static String formatQuantity(double quantity) {
        return BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
    }

    public static String formatSparkline(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        double min = values.get(0);
        double max = values.get(0);
        for (double value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        StringBuilder sparkline = new StringBuilder();
        for (double value : values) {
            if (max == min) {
                sparkline.append('▄');
            } else {
                // position of the value between min and max, from 0.0 to 1.0
                double ratio = (value - min) / (max - min);
                int index = (int) Math.round(ratio * (SPARKLINE_LEVELS.length - 1));
                sparkline.append(SPARKLINE_LEVELS[index]);
            }
        }
        return sparkline.toString();
    }
}
