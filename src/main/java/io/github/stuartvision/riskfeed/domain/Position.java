package io.github.stuartvision.riskfeed.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A loaded position (JPA/Hibernate entity). Amounts are in the trade currency, plus USD. */
@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_id", nullable = false)
    private long importId;

    @Column(name = "trade_id", nullable = false, length = 30)
    private String tradeId;

    @Column(nullable = false, length = 30)
    private String book;

    @Column(name = "counterparty_lei", nullable = false, length = 20)
    private String counterpartyLei;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Product product;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal notional;

    @Column(name = "market_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "market_value_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal marketValueUsd;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    protected Position() {
        // for JPA
    }

    public Position(long importId, ValidPosition p, BigDecimal marketValueUsd) {
        this.importId = importId;
        this.tradeId = p.tradeId();
        this.book = p.book();
        this.counterpartyLei = p.counterpartyLei();
        this.product = p.product();
        this.currency = p.currency();
        this.notional = p.notional();
        this.marketValue = p.marketValue();
        this.marketValueUsd = marketValueUsd;
        this.asOfDate = p.asOfDate();
    }

    public Long getId() { return id; }
    public long getImportId() { return importId; }
    public String getTradeId() { return tradeId; }
    public String getBook() { return book; }
    public String getCounterpartyLei() { return counterpartyLei; }
    public Product getProduct() { return product; }
    public String getCurrency() { return currency; }
    public BigDecimal getNotional() { return notional; }
    public BigDecimal getMarketValue() { return marketValue; }
    public BigDecimal getMarketValueUsd() { return marketValueUsd; }
    public LocalDate getAsOfDate() { return asOfDate; }
}
