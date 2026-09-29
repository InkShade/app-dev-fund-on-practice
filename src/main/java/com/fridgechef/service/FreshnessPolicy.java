package com.fridgechef.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.fridgechef.config.FridgeChefProperties;
import com.fridgechef.domain.FreshnessStatus;

/** Decides how fresh a product is, based on today's date and its expiry date. */
@Component
public class FreshnessPolicy {

    private final Clock clock;
    private final int expiringSoonDays;

    public FreshnessPolicy(Clock clock, FridgeChefProperties properties) {
        this.clock = clock;
        this.expiringSoonDays = properties.expiringSoonDays();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public long daysLeft(LocalDate expiryDate) {
        return ChronoUnit.DAYS.between(today(), expiryDate);
    }

    public FreshnessStatus statusOf(LocalDate expiryDate) {
        long daysLeft = daysLeft(expiryDate);
        if (daysLeft < 0) {
            return FreshnessStatus.EXPIRED;
        }
        if (daysLeft <= expiringSoonDays) {
            return FreshnessStatus.EXPIRING_SOON;
        }
        return FreshnessStatus.FRESH;
    }
}
