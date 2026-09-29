package com.fridgechef.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Application settings bound from the {@code fridgechef.*} properties.
 *
 * @param expiringSoonDays a product counts as "expiring soon" when it expires within this many days
 */
@Validated
@ConfigurationProperties(prefix = "fridgechef")
public record FridgeChefProperties(@PositiveOrZero int expiringSoonDays) {
}
