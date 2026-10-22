-- Counterparty exposure report for one business date, for reporting tools and
-- downstream risk jobs. Net = sum of market values (USD); gross = sum of
-- absolute market values. Served by ix_positions_date_cpty (covering index).
CREATE OR ALTER PROCEDURE dbo.usp_counterparty_exposure
    @AsOfDate    DATE,
    @MinGrossUsd DECIMAL(19, 2) = 0
AS
BEGIN
    SET NOCOUNT ON;

    SELECT counterparty_lei,
           COUNT(*)                   AS positions,
           SUM(market_value_usd)      AS net_usd,
           SUM(ABS(market_value_usd)) AS gross_usd
    FROM dbo.positions
    WHERE as_of_date = @AsOfDate
    GROUP BY counterparty_lei
    HAVING SUM(ABS(market_value_usd)) >= @MinGrossUsd
    ORDER BY gross_usd DESC;
END;
