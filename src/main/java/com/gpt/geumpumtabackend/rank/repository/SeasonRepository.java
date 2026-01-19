package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {


    @Query(value = """
      SELECT *
      FROM season
      WHERE start_date <= :date AND end_date >= :date
    """, nativeQuery = true)
    Optional<Season> findByDateRange(@Param("date") LocalDate date);
}
