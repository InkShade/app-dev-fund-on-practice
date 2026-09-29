package com.fridgechef.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fridgechef.domain.MeasureUnit;

/** Formats quantities for display, e.g. {@code 200.0 GRAM -> "200 g"}. */
public final class Quantities {

    private Quantities() {
    }

    public static String format(double quantity, MeasureUnit unit) {
        String amount = BigDecimal.valueOf(quantity)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
        return amount + " " + unit.getSymbol();
    }
}
