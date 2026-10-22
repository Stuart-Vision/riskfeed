package io.github.stuartvision.riskfeed.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Checks a raw record and converts it to typed values. Reports every problem
 * in the record at once, so the source system can fix them in one go.
 */
@Component
public class PositionValidator {

    private static final Pattern TRADE_ID = Pattern.compile("[A-Z0-9][A-Z0-9-]{2,29}");
    private static final Pattern BOOK = Pattern.compile("[A-Z0-9_]{2,30}");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1E15");

    private final Clock clock;

    public PositionValidator(Clock clock) {
        this.clock = clock;
    }

    public ValidPosition validate(RawPosition raw) {
        List<String> errors = new ArrayList<>();

        String tradeId = trim(raw.tradeId());
        if (!TRADE_ID.matcher(tradeId).matches()) {
            errors.add("tradeId '" + tradeId + "' must be 3-30 upper-case letters, digits or '-'");
        }
        String book = trim(raw.book());
        if (!BOOK.matcher(book).matches()) {
            errors.add("book '" + book + "' is invalid");
        }
        String lei = trim(raw.counterpartyLei());
        if (!Lei.isValid(lei)) {
            errors.add("counterpartyLei '" + lei + "' is not a valid LEI");
        }
        Product product = null;
        try {
            product = Product.valueOf(trim(raw.product()));
        } catch (IllegalArgumentException e) {
            errors.add("product '" + trim(raw.product()) + "' is not one of " + Arrays.toString(Product.values()));
        }
        String currency = trim(raw.currency());
        if (!CURRENCY.matcher(currency).matches()) {
            errors.add("currency '" + currency + "' is not an ISO 4217 code");
        }
        BigDecimal notional = amount(raw.notional(), "notional", errors);
        if (notional != null && notional.signum() <= 0) {
            errors.add("notional must be positive");
        }
        BigDecimal marketValue = amount(raw.marketValue(), "marketValue", errors);
        LocalDate asOfDate = null;
        try {
            asOfDate = LocalDate.parse(trim(raw.asOfDate()));
            if (asOfDate.isAfter(LocalDate.now(clock))) {
                errors.add("asOfDate " + asOfDate + " is in the future");
            }
        } catch (DateTimeParseException e) {
            errors.add("asOfDate '" + trim(raw.asOfDate()) + "' must be YYYY-MM-DD");
        }

        if (!errors.isEmpty()) {
            throw new InvalidPositionException(String.join("; ", errors));
        }
        return new ValidPosition(tradeId, book, lei, product, currency, notional, marketValue, asOfDate);
    }

    private static BigDecimal amount(String text, String field, List<String> errors) {
        try {
            BigDecimal value = new BigDecimal(trim(text));
            if (value.abs().compareTo(MAX_AMOUNT) >= 0 || value.scale() > 2) {
                errors.add(field + " " + value.toPlainString() + " is out of range or has more than 2 decimals");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            errors.add(field + " '" + trim(text) + "' is not a number");
            return null;
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.strip();
    }
}
