package org.ilias.influapp.utils;

public class NumberFormatUtils {

    public static String compact(Number n) {
        if (n == null) return "0";
        double v = n.doubleValue();
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0";
        if (Math.abs(v) >= 1_000_000) {
            return String.format("%.1fM", v / 1_000_000.0);
        }
        if (Math.abs(v) >= 1000) {
            return String.format("%.1fk", v / 1000.0);
        }
        if (v == Math.floor(v)) {
            return String.format("%d", (long) v);
        }
        return String.format("%.2f", v);
    }

    public static String twoDecimals(Number n) {
        if (n == null) return "0.00";
        double v = n.doubleValue();
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0.00";
        return String.format("%.2f", v);
    }
}

