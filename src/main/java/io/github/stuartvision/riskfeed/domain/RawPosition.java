package io.github.stuartvision.riskfeed.domain;

/**
 * One record exactly as read from a CSV or XML file: all text, not yet validated.
 *
 * @param recordNo line number (CSV) or element number (XML), for error reports
 */
public record RawPosition(
        int recordNo,
        String tradeId,
        String book,
        String counterpartyLei,
        String product,
        String currency,
        String notional,
        String marketValue,
        String asOfDate) {
}
