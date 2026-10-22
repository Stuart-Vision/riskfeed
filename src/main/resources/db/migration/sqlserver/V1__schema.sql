-- RiskFeed schema for SQL Server.

-- Daily FX rates used to convert market values to USD.
CREATE TABLE dbo.fx_rates (
    currency    VARCHAR(3)     NOT NULL,
    as_of_date  DATE           NOT NULL,
    usd_rate    DECIMAL(19, 8) NOT NULL CHECK (usd_rate > 0),
    CONSTRAINT pk_fx_rates PRIMARY KEY (currency, as_of_date)
);

-- One row per trade per business date, loaded from position files.
CREATE TABLE dbo.positions (
    id                BIGINT IDENTITY(1, 1) PRIMARY KEY,
    import_id         BIGINT         NOT NULL,  -- Spring Batch job execution that loaded the row
    trade_id          VARCHAR(30)    NOT NULL,
    book              VARCHAR(30)    NOT NULL,
    counterparty_lei  VARCHAR(20)    NOT NULL,
    product           VARCHAR(20)    NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    notional          DECIMAL(19, 2) NOT NULL,
    market_value      DECIMAL(19, 2) NOT NULL,
    market_value_usd  DECIMAL(19, 2) NOT NULL,
    as_of_date        DATE           NOT NULL,
    CONSTRAINT uq_positions_trade_date UNIQUE (trade_id, as_of_date)
);

-- Covering index for the exposure report: seek on date, group by counterparty,
-- and read market_value_usd from the index without touching the table.
CREATE INDEX ix_positions_date_cpty ON dbo.positions (as_of_date, counterparty_lei) INCLUDE (market_value_usd);
CREATE INDEX ix_positions_import ON dbo.positions (import_id);

-- Records that failed validation, with the reason, so they can be fixed at source.
CREATE TABLE dbo.rejections (
    id         BIGINT IDENTITY(1, 1) PRIMARY KEY,
    import_id  BIGINT        NOT NULL,
    record_no  INT           NOT NULL,
    trade_id   VARCHAR(100)  NULL,
    reason     VARCHAR(1000) NOT NULL
);
CREATE INDEX ix_rejections_import ON dbo.rejections (import_id);
