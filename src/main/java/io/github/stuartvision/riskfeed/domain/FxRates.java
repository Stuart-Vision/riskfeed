package io.github.stuartvision.riskfeed.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** USD conversion rates by currency and business date. */
@Component
public class FxRates {

    private final JdbcClient jdbc;

    public FxRates(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** USD per one unit of {@code currency} on {@code date}, if a rate was loaded. */
    public Optional<BigDecimal> usdRate(String currency, LocalDate date) {
        return jdbc.sql("SELECT usd_rate FROM fx_rates WHERE currency = :currency AND as_of_date = :date")
                .param("currency", currency)
                .param("date", date)
                .query(BigDecimal.class)
                .optional();
    }
}
