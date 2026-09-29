package com.fridgechef.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fridgechef.domain.MeasureUnit;

class QuantitiesTest {

    @ParameterizedTest
    @CsvSource({
            "200.0, GRAM, 200 g",
            "1.5, MILLILITER, 1.5 ml",
            "0.3333, PIECE, 0.33 pcs",
            "0, GRAM, 0 g",
            "1000, MILLILITER, 1000 ml"
    })
    void formatsWithoutTrailingZeros(double quantity, MeasureUnit unit, String expected) {
        assertThat(Quantities.format(quantity, unit)).isEqualTo(expected);
    }
}
