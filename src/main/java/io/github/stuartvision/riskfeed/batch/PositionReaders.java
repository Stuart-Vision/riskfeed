package io.github.stuartvision.riskfeed.batch;

import java.nio.file.Path;
import java.util.Locale;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import io.github.stuartvision.riskfeed.domain.RawPosition;

/**
 * Readers for the two supported file formats. Both produce {@link RawPosition}s,
 * so the rest of the job doesn't care where a record came from.
 *
 * <pre>
 * CSV:  tradeId,book,counterpartyLei,product,currency,notional,marketValue,asOfDate   (header line required)
 * XML:  &lt;positions&gt;&lt;position&gt;&lt;tradeId&gt;...&lt;/tradeId&gt;...&lt;/position&gt;&lt;/positions&gt;
 * </pre>
 */
public final class PositionReaders {

    static final String[] COLUMNS = {
            "tradeId", "book", "counterpartyLei", "product", "currency", "notional", "marketValue", "asOfDate"};

    private PositionReaders() {
    }

    public static boolean isSupported(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") || name.endsWith(".xml");
    }

    static ItemStreamReader<RawPosition> forFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            return csv(file);
        }
        if (name.endsWith(".xml")) {
            return xml(file);
        }
        throw new IllegalArgumentException("Unsupported file type: " + file.getFileName());
    }

    static FlatFileItemReader<RawPosition> csv(Path file) {
        var tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(COLUMNS);
        // Not strict: a short row becomes empty fields, which the validator then reports by name.
        tokenizer.setStrict(false);
        return new FlatFileItemReaderBuilder<RawPosition>()
                .name("csvPositionReader")
                .resource(new FileSystemResource(file))
                .linesToSkip(1)
                .strict(true) // a missing file fails the job
                .saveState(false)
                .lineMapper((line, lineNumber) -> {
                    FieldSet f = tokenizer.tokenize(line);
                    return new RawPosition(lineNumber, f.readString("tradeId"), f.readString("book"),
                            f.readString("counterpartyLei"), f.readString("product"), f.readString("currency"),
                            f.readString("notional"), f.readString("marketValue"), f.readString("asOfDate"));
                })
                .build();
    }

    static ItemStreamReader<RawPosition> xml(Path file) {
        var marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(PositionXml.class);
        try {
            marshaller.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        StaxEventItemReader<PositionXml> delegate = new StaxEventItemReaderBuilder<PositionXml>()
                .name("xmlPositionReader")
                .resource(new FileSystemResource(file))
                .addFragmentRootElements("position")
                .unmarshaller(marshaller)
                .strict(true)
                .saveState(false)
                .build();
        return new NumberingXmlReader(delegate);
    }

    /** Numbers XML records (1, 2, 3...) so rejections can point at the right element. */
    private static final class NumberingXmlReader implements ItemStreamReader<RawPosition> {

        private final StaxEventItemReader<PositionXml> delegate;
        private int count;

        NumberingXmlReader(StaxEventItemReader<PositionXml> delegate) {
            this.delegate = delegate;
        }

        @Override
        public RawPosition read() throws Exception {
            PositionXml x = delegate.read();
            if (x == null) {
                return null;
            }
            return new RawPosition(++count, x.tradeId, x.book, x.counterpartyLei, x.product, x.currency,
                    x.notional, x.marketValue, x.asOfDate);
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
}
