package io.github.stuartvision.riskfeed.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RejectionRepository extends JpaRepository<Rejection, Long> {

    List<Rejection> findByImportIdOrderByRecordNo(long importId);
}
