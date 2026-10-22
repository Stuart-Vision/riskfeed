package io.github.stuartvision.riskfeed.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A record that passed validation, with typed fields. */
public record ValidPosition(
        String tradeId,
        String book,
        String counterpartyLei,
        Product product,
        String currency,
        BigDecimal notional,
        BigDecimal marketValue,
        LocalDate asOfDate) {
}
