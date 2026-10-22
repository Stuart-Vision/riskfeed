package io.github.stuartvision.riskfeed.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<Position, Long> {

    boolean existsByTradeIdAndAsOfDate(String tradeId, LocalDate asOfDate);

    long countByImportId(long importId);

    @Query("""
            select new io.github.stuartvision.riskfeed.domain.CounterpartyExposure(
                       p.counterpartyLei, count(p), sum(p.marketValueUsd), sum(abs(p.marketValueUsd)))
            from Position p
            where p.asOfDate = :asOfDate
            group by p.counterpartyLei
            order by sum(abs(p.marketValueUsd)) desc
            """)
    List<CounterpartyExposure> exposuresOn(@Param("asOfDate") LocalDate asOfDate);
}
