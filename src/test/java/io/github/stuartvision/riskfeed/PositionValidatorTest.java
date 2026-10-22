package io.github.stuartvision.riskfeed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.stuartvision.riskfeed.domain.InvalidPositionException;
import io.github.stuartvision.riskfeed.domain.PositionValidator;
import io.github.stuartvision.riskfeed.domain.Product;
import io.github.stuartvision.riskfeed.domain.RawPosition;
import io.github.stuartvision.riskfeed.domain.ValidPosition;

class PositionValidatorTest {

    private static final String LEI = "5493001KJTIIGC8Y1R12";

    // "Today" is fixed so the future-date rule is testable.
    private final PositionValidator validator = new PositionValidator(
            Clock.fixed(LocalDate.of(2026, 10, 22).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    private static RawPosition raw(String tradeId, String book, String lei, String product, String currency,
            String notional, String marketValue, String asOfDate) {
        return new RawPosition(2, tradeId, book, lei, product, currency, notional, marketValue, asOfDate);
    }

    private static RawPosition valid() {
        return raw("FXD-1001", "FX_SG", LEI, "FX_FORWARD", "SGD", "5000000.00", "-125000.50", "2026-10-21");
    }

    @Test
    void convertsAValidRecord() {
        ValidPosition p = validator.validate(valid());

        assertThat(p.tradeId()).isEqualTo("FXD-1001");
        assertThat(p.product()).isEqualTo(Product.FX_FORWARD);
        assertThat(p.notional()).isEqualByComparingTo("5000000");
        assertThat(p.marketValue()).isEqualByComparingTo(new BigDecimal("-125000.50"));
        assertThat(p.asOfDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void trimsWhitespaceAroundFields() {
        ValidPosition p = validator.validate(
                raw(" FXD-1001 ", "FX_SG ", " " + LEI, "FX_FORWARD", " SGD", " 1.00 ", "2", " 2026-10-21 "));
        assertThat(p.tradeId()).isEqualTo("FXD-1001");
        assertThat(p.currency()).isEqualTo("SGD");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            bad trade id      | fx#1     | FX_SG | 5493001KJTIIGC8Y1R12 | IRS      | USD | 1    | 1     | 2026-10-21 | tradeId
            bad LEI checksum  | T-1      | FX_SG | 5493001KJTIIGC8Y1R13 | IRS      | USD | 1    | 1     | 2026-10-21 | not a valid LEI
            unknown product   | T-1      | FX_SG | 5493001KJTIIGC8Y1R12 | SWAPTION | USD | 1    | 1     | 2026-10-21 | product 'SWAPTION'
            bad currency      | T-1      | FX_SG | 5493001KJTIIGC8Y1R12 | IRS      | us$ | 1    | 1     | 2026-10-21 | ISO 4217
            zero notional     | T-1      | FX_SG | 5493001KJTIIGC8Y1R12 | IRS      | USD | 0    | 1     | 2026-10-21 | notional must be positive
            text amount       | T-1      | FX_SG | 5493001KJTIIGC8Y1R12 | IRS      | USD | 1    | abc   | 2026-10-21 | marketValue 'abc' is not a number
            three decimals    | T-1      | FX_SG | 5493001KJTIIGC8Y1R12 | IRS      | USD | 1    | 1.005 | 2026-10-21 | more than 2 decimals
            wrong date format | T-1      | FX_SG | 5493001KJTIIGC8Y1R12 | IRS      | USD | 1    | 1     | 21/10/2026 | must be YYYY-MM-DD
            future date       | T-1      | FX_SG | 5493001KJTIIGC8Y1R12 | IRS      | USD | 1    | 1     | 2026-10-23 | in the future
            """)
    void rejectsInvalidField(String rule, String tradeId, String book, String lei, String product, String currency,
            String notional, String marketValue, String asOfDate, String expectedMessage) {
        assertThatThrownBy(() -> validator.validate(raw(tradeId, book, lei, product, currency, notional, marketValue, asOfDate)))
                .isInstanceOf(InvalidPositionException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    void reportsEveryProblemInARecordAtOnce() {
        assertThatThrownBy(() -> validator.validate(raw("T-1", "RATES", LEI, "SWAPTION", "USD", "-5", "abc", "21/10/2026")))
                .hasMessageContaining("product")
                .hasMessageContaining("notional must be positive")
                .hasMessageContaining("marketValue")
                .hasMessageContaining("asOfDate");
    }

    @Test
    void treatsMissingFieldsAsInvalidNotAsCrashes() {
        assertThatThrownBy(() -> validator.validate(new RawPosition(5, null, null, null, null, null, null, null, null)))
                .isInstanceOf(InvalidPositionException.class)
                .hasMessageContaining("tradeId");
    }
}
