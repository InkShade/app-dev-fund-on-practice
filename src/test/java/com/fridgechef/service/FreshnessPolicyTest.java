package com.fridgechef.service;

import static com.fridgechef.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fridgechef.TestData;
import com.fridgechef.domain.FreshnessStatus;

class FreshnessPolicyTest {

    private final FreshnessPolicy policy = TestData.freshnessPolicy();

    @Test
    void todayComesFromTheClock() {
        assertThat(policy.today()).isEqualTo(TODAY);
    }

    @ParameterizedTest(name = "{0} days left -> {1}")
    @CsvSource({
            "-5, EXPIRED",
            "-1, EXPIRED",
            "0, EXPIRING_SOON",
            "3, EXPIRING_SOON",
            "4, FRESH",
            "30, FRESH"
    })
    void statusDependsOnDaysLeft(int daysLeft, FreshnessStatus expected) {
        assertThat(policy.statusOf(TODAY.plusDays(daysLeft))).isEqualTo(expected);
    }

    @Test
    void daysLeftIsNegativeForExpiredProducts() {
        assertThat(policy.daysLeft(TODAY.plusDays(2))).isEqualTo(2);
        assertThat(policy.daysLeft(TODAY.minusDays(3))).isEqualTo(-3);
    }
}
