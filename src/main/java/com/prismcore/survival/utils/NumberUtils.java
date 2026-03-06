package com.prismcore.survival.utils;

import java.text.DecimalFormat;

public class NumberUtils {

    private static final DecimalFormat DF = new DecimalFormat("#.#");

    public static String format(double number) {
        if (number >= 1_000_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000_000.0, "t");
        } else if (number >= 1_000_000_000.0) {
            return formatWithSuffix(number, 1_000_000_000.0, "B");
        } else if (number >= 1_000_000.0) {
            return formatWithSuffix(number, 1_000_000.0, "M");
        } else if (number >= 1_000.0) {
            return formatWithSuffix(number, 1_000.0, "K");
        } else {
            return DF.format(number);
        }
    }

    private static String formatWithSuffix(double number, double divisor, String suffix) {
        double scaled = number / divisor;
        scaled = Math.floor(scaled * 10) / 10.0;
        return DF.format(scaled) + suffix;
    }

    public static String formatMoney(double amount) {
        return "$" + format(amount);
    }
}
