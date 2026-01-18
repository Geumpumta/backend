package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {


    @Query("""
        SELECT s FROM Season s
        WHERE s.startDate <= :date
          AND s.endDate >= :date
        ORDER BY s.createdAt DESC
        LIMIT 1
        """)
    Optional<Season> findByDateRange(@Param("date") LocalDate date);
}
