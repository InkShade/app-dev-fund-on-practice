package com.fridgechef.domain;

public enum MeasureUnit {
    GRAM("g"),
    MILLILITER("ml"),
    PIECE("pcs");

    private final String symbol;

    MeasureUnit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
