package io.github.stuartvision.riskfeed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.stuartvision.riskfeed.batch.AlreadyImportedException;
import io.github.stuartvision.riskfeed.batch.ImportService;
import io.github.stuartvision.riskfeed.batch.ImportSummary;
import io.github.stuartvision.riskfeed.domain.CounterpartyExposure;
import io.github.stuartvision.riskfeed.domain.PositionRepository;
import io.github.stuartvision.riskfeed.domain.Rejection;
import io.github.stuartvision.riskfeed.domain.RejectionRepository;

/** Runs the real Spring Batch job against an in-memory H2 database (SQL Server mode). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:job-${random.uuid};MODE=MSSQLServer",
        "riskfeed.inbox=target/test-inbox",
        "riskfeed.chunk-size=3"}) // small chunks so a file spans several transactions
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PositionImportJobTest {

    static final String L1 = "5493001KJTIIGC8Y1R12";
    static final String L2 = "549300A1B2C3D4E5F690";
    static final String L3 = "213800QWERTYUIOP1289";
    static final String L4 = "969500ZXCVBNMASDFG86";
    static final String HEADER = "tradeId,book,counterpartyLei,product,currency,notional,marketValue,asOfDate\n";

    @Autowired ImportService imports;
    @Autowired PositionRepository positions;
    @Autowired RejectionRepository rejections;

    @TempDir Path tmp;

    @Test
    void importsTheSampleCsvSkippingBadRecordsWithReasons() {
        ImportSummary summary = imports.importFile(Path.of("samples/positions-2026-10-21.csv"), "positions-2026-10-21.csv");

        assertThat(summary.status()).isEqualTo("COMPLETED_WITH_REJECTIONS");
        assertThat(summary.read()).isEqualTo(12);
        assertThat(summary.loaded()).isEqualTo(8);
        assertThat(summary.rejected()).isEqualTo(4);
        assertThat(summary.exitCode()).isEqualTo(3);
        assertThat(positions.countByImportId(summary.importId())).isEqualTo(8);

        Map<Integer, String> reasons = rejections.findByImportIdOrderByRecordNo(summary.importId()).stream()
                .collect(Collectors.toMap(Rejection::getRecordNo, Rejection::getReason));
        assertThat(reasons).containsOnlyKeys(9, 10, 11, 12); // file line numbers (line 1 is the header)
        assertThat(reasons.get(9)).contains("not a valid LEI");
        assertThat(reasons.get(10)).contains("FXD-1001 appears more than once");
        assertThat(reasons.get(11)).contains("no FX rate for CHF");
        assertThat(reasons.get(12)).contains("SWAPTION", "notional must be positive", "marketValue", "asOfDate");

        // Exposures in USD, largest gross first. Worked out by hand from the sample and the seeded FX rates.
        List<CounterpartyExposure> exposures = positions.exposuresOn(LocalDate.of(2026, 10, 21));
        assertThat(exposures).extracting(CounterpartyExposure::counterpartyLei).containsExactly(L3, L4, L2, L1);
        CounterpartyExposure l1 = exposures.get(3);
        assertThat(l1.positions()).isEqualTo(2);
        assertThat(l1.netUsd()).isEqualByComparingTo("49325.00");    // 125,000 SGD x 0.741 - 40,000 EUR x 1.0825
        assertThat(l1.grossUsd()).isEqualByComparingTo("135925.00"); // 92,625 + 43,300
        assertThat(exposures.get(1).netUsd()).isEqualByComparingTo("3620460.00"); // JPY and LKR loans

        // The same content is refused, even under another name.
        assertThatThrownBy(() -> imports.importFile(Path.of("samples/positions-2026-10-21.csv"), "again.csv"))
                .isInstanceOf(AlreadyImportedException.class);
    }

    @Test
    void importsXml() {
        ImportSummary summary = imports.importFile(Path.of("samples/positions-2026-10-22.xml"), "positions-2026-10-22.xml");

        assertThat(summary.read()).isEqualTo(4);
        assertThat(summary.loaded()).isEqualTo(3);
        assertThat(rejections.findByImportIdOrderByRecordNo(summary.importId()))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getRecordNo()).isEqualTo(4);
                    assertThat(r.getTradeId()).isEqualTo("LN-4104");
                    assertThat(r.getReason()).contains("notional must be positive");
                });
    }

    @Test
    void rejectsTradesAlreadyLoadedByAnEarlierFile() throws IOException {
        Path first = write("first.csv", HEADER + "DUP-1," + "RATES," + L2 + ",IRS,USD,100.00,10.00,2026-10-19\n");
        Path second = write("second.csv", HEADER
                + "DUP-1,RATES," + L2 + ",IRS,USD,999.00,99.00,2026-10-19\n"
                + "NEW-1,RATES," + L2 + ",IRS,USD,100.00,10.00,2026-10-19\n");

        assertThat(imports.importFile(first, "first.csv").status()).isEqualTo("COMPLETED");
        ImportSummary summary = imports.importFile(second, "second.csv");

        assertThat(summary.loaded()).isEqualTo(1);
        assertThat(rejections.findByImportIdOrderByRecordNo(summary.importId()))
                .singleElement()
                .extracting(Rejection::getReason)
                .asString().contains("DUP-1 was already loaded for 2026-10-19");
    }

    @Test
    void shortRowsAreRejectedNotFatal() throws IOException {
        Path file = write("short.csv", HEADER
                + "OK-1,RATES," + L1 + ",IRS,USD,100.00,10.00,2026-10-20\n"
                + "SHORT-1,RATES\n");

        ImportSummary summary = imports.importFile(file, "short.csv");

        assertThat(summary.status()).isEqualTo("COMPLETED_WITH_REJECTIONS");
        assertThat(summary.loaded()).isEqualTo(1);
        assertThat(rejections.findByImportIdOrderByRecordNo(summary.importId()))
                .singleElement().extracting(Rejection::getRecordNo).isEqualTo(3);
    }

    @Test
    void aBrokenXmlFileFailsTheImport() throws IOException {
        Path file = write("broken.xml", "<positions><position><tradeId>X-1</tradeId>");

        ImportSummary summary = imports.importFile(file, "broken.xml");

        assertThat(summary.status()).isEqualTo("FAILED");
        assertThat(summary.exitCode()).isEqualTo(1);
        assertThat(summary.error()).isNotBlank();
    }

    private Path write(String name, String content) throws IOException {
        return Files.writeString(tmp.resolve(name), content);
    }
}
