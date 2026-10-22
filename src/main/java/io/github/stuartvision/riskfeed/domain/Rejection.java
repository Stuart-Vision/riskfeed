package io.github.stuartvision.riskfeed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A record that was skipped during an import, and why. */
@Entity
@Table(name = "rejections")
public class Rejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_id", nullable = false)
    private long importId;

    @Column(name = "record_no", nullable = false)
    private int recordNo;

    @Column(name = "trade_id", length = 100)
    private String tradeId;

    @Column(nullable = false, length = 1000)
    private String reason;

    protected Rejection() {
        // for JPA
    }

    public Rejection(long importId, int recordNo, String tradeId, String reason) {
        this.importId = importId;
        this.recordNo = recordNo;
        this.tradeId = tradeId == null || tradeId.isBlank() ? null : truncate(tradeId.strip(), 100);
        this.reason = truncate(reason, 1000);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    public long getImportId() { return importId; }
    public int getRecordNo() { return recordNo; }
    public String getTradeId() { return tradeId; }
    public String getReason() { return reason; }
}
