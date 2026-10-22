package io.github.stuartvision.riskfeed.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;

import io.github.stuartvision.riskfeed.domain.FxRates;
import io.github.stuartvision.riskfeed.domain.InvalidPositionException;
import io.github.stuartvision.riskfeed.domain.Position;
import io.github.stuartvision.riskfeed.domain.PositionRepository;
import io.github.stuartvision.riskfeed.domain.PositionValidator;
import io.github.stuartvision.riskfeed.domain.RawPosition;
import io.github.stuartvision.riskfeed.domain.ValidPosition;

/**
 * Validates a record, rejects duplicates and converts its market value to USD.
 * Throws {@link InvalidPositionException} for bad records; the step skips them.
 * One instance per step execution (step scope), so duplicate tracking is per file.
 */
public class PositionProcessor implements ItemProcessor<RawPosition, Position> {

    private final PositionValidator validator;
    private final FxRates fxRates;
    private final PositionRepository positions;
    private final long importId;
    private final Map<String, Integer> firstSeenAt = new HashMap<>();

    public PositionProcessor(PositionValidator validator, FxRates fxRates, PositionRepository positions, long importId) {
        this.validator = validator;
        this.fxRates = fxRates;
        this.positions = positions;
        this.importId = importId;
    }

    @Override
    public Position process(RawPosition raw) {
        ValidPosition p = validator.validate(raw);

        // When a record in a chunk is skipped, Spring Batch rolls the chunk back and
        // processes its other records again. Remembering the record number of each
        // trade's first appearance keeps that second pass from looking like a duplicate.
        Integer firstRecord = firstSeenAt.putIfAbsent(p.tradeId() + "|" + p.asOfDate(), raw.recordNo());
        if (firstRecord != null && firstRecord != raw.recordNo()) {
            throw new InvalidPositionException("trade " + p.tradeId() + " appears more than once for " + p.asOfDate()
                    + " in this file (first at record " + firstRecord + ")");
        }
        if (positions.existsByTradeIdAndAsOfDate(p.tradeId(), p.asOfDate())) {
            throw new InvalidPositionException("trade " + p.tradeId() + " was already loaded for " + p.asOfDate());
        }

        BigDecimal rate = fxRates.usdRate(p.currency(), p.asOfDate())
                .orElseThrow(() -> new InvalidPositionException("no FX rate for " + p.currency() + " on " + p.asOfDate()));
        // Banker's rounding (HALF_EVEN) avoids a systematic bias when many amounts are rounded.
        BigDecimal usd = p.marketValue().multiply(rate).setScale(2, RoundingMode.HALF_EVEN);

        return new Position(importId, p, usd);
    }
}
