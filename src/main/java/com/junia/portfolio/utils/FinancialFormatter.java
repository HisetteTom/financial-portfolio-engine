package com.junia.portfolio.utils;

import java.util.List;

public class FinancialFormatter {

    public static String formatBarChart(double percentage, int totalWidth) {
        double clamped = Math.max(0.0, Math.min(100.0, percentage));
        int filledCount = (int) Math.round((clamped / 100.0) * totalWidth);
        int emptyCount = totalWidth - filledCount;

        return "[" + "█".repeat(filledCount) + " ".repeat(emptyCount) + "]";
    }

    public static String formatSparkline(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (values.size() == 1) {
            return "▄";
        }

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        char[] levels = new char[]{' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'};
        StringBuilder sb = new StringBuilder();

        for (double v : values) {
            if (max == min) {
                sb.append('▄');
            } else {
                int index = (int) Math.round(((v - min) / (max - min)) * (levels.length - 1));
                sb.append(levels[index]);
            }
        }
        return sb.toString();
    }
}
