-- RiskFeed schema for H2 (local runs and tests). Same tables as the SQL Server
-- version; H2 has no INCLUDE columns, so the report index is a plain composite one.
CREATE TABLE fx_rates (
    currency    VARCHAR(3)     NOT NULL,
    as_of_date  DATE           NOT NULL,
    usd_rate    DECIMAL(19, 8) NOT NULL CHECK (usd_rate > 0),
    CONSTRAINT pk_fx_rates PRIMARY KEY (currency, as_of_date)
);

CREATE TABLE positions (
    id                BIGINT IDENTITY(1, 1) PRIMARY KEY,
    import_id         BIGINT         NOT NULL,
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
CREATE INDEX ix_positions_date_cpty ON positions (as_of_date, counterparty_lei, market_value_usd);
CREATE INDEX ix_positions_import ON positions (import_id);

CREATE TABLE rejections (
    id         BIGINT IDENTITY(1, 1) PRIMARY KEY,
    import_id  BIGINT        NOT NULL,
    record_no  INT           NOT NULL,
    trade_id   VARCHAR(100)  NULL,
    reason     VARCHAR(1000) NOT NULL
);
CREATE INDEX ix_rejections_import ON rejections (import_id);
