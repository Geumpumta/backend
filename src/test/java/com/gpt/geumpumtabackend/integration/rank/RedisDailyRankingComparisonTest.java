package com.gpt.geumpumtabackend.integration.rank;

import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.service.RedisDailyRanking;
import com.gpt.geumpumtabackend.study.event.StudySessionChanged;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Testcontainers
class RedisDailyRankingComparisonTest {
    @Container static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0");
    @Container static final GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

    @Test
    @DisplayName("일간 Redis 결과는 추월·동점·자정·종료와 100위 경계에서 원본 SQL과 일치한다")
    void compareWithCurrentSql() throws Exception {
        var db = new NamedParameterJdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword()));
        var sql = db.getJdbcTemplate();
        sql.execute("CREATE TABLE user (id BIGINT PRIMARY KEY, role VARCHAR(20), nickname VARCHAR(20), picture VARCHAR(20), department VARCHAR(30))");
        sql.execute("CREATE TABLE study_session (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, start_time DATETIME(6), end_time DATETIME(6))");
        UserRepository users = mock(UserRepository.class);
        Map<Long,User> profiles = new HashMap<>();
        for (long id=1; id<=101; id++) {
            sql.update("INSERT INTO user (id,role) VALUES (?, 'USER')", id);
            var profile = mock(User.class);
            when(profile.getId()).thenReturn(id);
            profiles.put(id,profile);
        }
        when(users.findAllById(any())).thenAnswer(call -> {
            List<Long> ids = call.getArgument(0);
            return ids.stream().map(profiles::get).toList();
        });
        sql.update("INSERT INTO study_session(user_id,start_time,end_time) VALUES (1,'2026-01-01 23:00',NULL),(2,'2026-01-02 00:00','2026-01-02 00:50')");
        String query = StudySessionRepository.class.getMethod("calculateCurrentPeriodRanking",LocalDateTime.class,
                LocalDateTime.class,LocalDateTime.class).getAnnotation(Query.class).value();
        var factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        try {
            var ranking = new RedisDailyRanking(db,new StringRedisTemplate(factory),users);
            for (int minute : List.of(49,50,51,52)) {
                if (minute == 52) {
                    sql.update("UPDATE study_session SET end_time='2026-01-02 00:51:00.123500' WHERE user_id=1");
                    ranking.afterCommit(new StudySessionChanged(1));
                    ranking.refresh();
                }
                var now = LocalDateTime.of(2026,1,2,0,minute);
                var expected = db.query(query,Map.of("periodStart",now.toLocalDate().atStartOfDay(),
                        "periodEnd",now.toLocalDate().atTime(23,59,59),"now",now),
                        (rs,i)-> List.of(rs.getLong("ranking"),rs.getLong("totalMillis")));
                var actual = ranking.top(now);
                assertThat(actual).hasSize(100);
                assertThat(actual.stream().map(PersonalRankingTemp::getUserId).distinct().count()).isEqualTo(100);
                assertThat(actual.stream().map(r -> List.of(r.getRanking(),r.getTotalMillis())).toList()).isEqualTo(expected);
                assertThat(actual.get(0).getUserId()).isIn(minute < 50 ? List.of(2L) : minute == 50 ? List.of(1L,2L) : List.of(1L));
            }
        } finally {
            factory.destroy();
        }
    }
}
