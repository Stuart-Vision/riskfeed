package io.github.stuartvision.riskfeed.batch;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;

import io.github.stuartvision.riskfeed.domain.Position;
import io.github.stuartvision.riskfeed.domain.RawPosition;
import io.github.stuartvision.riskfeed.domain.Rejection;
import io.github.stuartvision.riskfeed.domain.RejectionRepository;

/**
 * Saves every skipped record with its reason. Spring Batch calls this inside the
 * chunk's transaction, so a rejection is stored only if its chunk commits.
 */
public class RejectionListener implements SkipListener<RawPosition, Position> {

    private final RejectionRepository rejections;
    private final long importId;

    public RejectionListener(RejectionRepository rejections, long importId) {
        this.rejections = rejections;
        this.importId = importId;
    }

    @Override
    public void onSkipInProcess(RawPosition item, Throwable t) {
        rejections.save(new Rejection(importId, item.recordNo(), item.tradeId(), t.getMessage()));
    }

    @Override
    public void onSkipInRead(Throwable t) {
        int line = t instanceof FlatFileParseException e ? e.getLineNumber() : 0;
        rejections.save(new Rejection(importId, line, null, "unreadable record: " + t.getMessage()));
    }
}
