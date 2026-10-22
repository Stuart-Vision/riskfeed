package io.github.stuartvision.riskfeed.batch;

import java.nio.file.Path;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

import io.github.stuartvision.riskfeed.domain.RawPosition;

/** Reads a CSV or XML position file, choosing the format from the file extension. */
public class PositionFileReader implements ItemStreamReader<RawPosition> {

    private final ItemStreamReader<RawPosition> delegate;

    public PositionFileReader(Path file) {
        this.delegate = PositionReaders.forFile(file);
    }

    @Override
    public RawPosition read() throws Exception {
        return delegate.read();
    }

    @Override
    public void open(ExecutionContext context) {
        delegate.open(context);
    }

    @Override
    public void update(ExecutionContext context) {
        delegate.update(context);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
