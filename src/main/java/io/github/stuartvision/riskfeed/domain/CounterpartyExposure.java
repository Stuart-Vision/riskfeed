package io.github.stuartvision.riskfeed.domain;

import java.math.BigDecimal;

/**
 * Exposure to one counterparty on one date, in USD.
 *
 * @param netUsd sum of market values (what we would receive minus what we would pay)
 * @param grossUsd sum of absolute market values (overall size of the relationship)
 */
public record CounterpartyExposure(String counterpartyLei, Long positions, BigDecimal netUsd, BigDecimal grossUsd) {
}
