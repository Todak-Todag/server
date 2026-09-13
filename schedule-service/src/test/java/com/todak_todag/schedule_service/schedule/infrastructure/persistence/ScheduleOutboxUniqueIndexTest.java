package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ScheduleOutboxEventCommandRepositoryImpl;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 아웃박스 부분 유니크 인덱스 검증
// 테스트 DB는 ddl-auto=create-drop이라 운영 DDL(docker/postgres/schedule-service.sql)이 적용되지 않으므로
// 같은 인덱스를 여기서 직접 만들어 검증
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, ScheduleOutboxEventCommandRepositoryImpl.class})
class ScheduleOutboxUniqueIndexTest extends PostgresTestSupport {

    // docker/postgres/schedule-service.sql의 ux_schedule_outbox_events_care_plan_completed와 동일해야 함
    private static final String PARTIAL_UNIQUE_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_outbox_events_care_plan_completed
                ON schedule_schema.p_schedule_outbox_events (aggregate_id)
                WHERE event_type = 'CarePlanCompleted'
            """;

    @Autowired
    private ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void createPartialUniqueIndex() {
        entityManager.createNativeQuery(PARTIAL_UNIQUE_INDEX).executeUpdate();
    }

    @Test
    @DisplayName("같은 케어플랜으로 CarePlanCompleted를 두 번 적재하면 DB가 막는다")
    void 같은_케어플랜의_CarePlanCompleted는_두_번_적재되지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        scheduleOutboxEventCommandRepository.save(
                ScheduleOutboxEvent.create("CarePlanCompleted", carePlanId, "{}")
        );

        // when & then
        assertThatThrownBy(() -> scheduleOutboxEventCommandRepository.save(
                ScheduleOutboxEvent.create("CarePlanCompleted", carePlanId, "{}")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("케어플랜이 다르면 CarePlanCompleted가 여러 건 적재된다")
    void 다른_케어플랜의_CarePlanCompleted는_각각_적재된다() {
        // given
        scheduleOutboxEventCommandRepository.save(
                ScheduleOutboxEvent.create("CarePlanCompleted", UUID.randomUUID(), "{}")
        );

        // when & then
        assertThatCode(() -> scheduleOutboxEventCommandRepository.save(
                ScheduleOutboxEvent.create("CarePlanCompleted", UUID.randomUUID(), "{}")
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ProviderReMatched는 같은 aggregateId로 다시 적재할 수 있다 — 일정 재변경 경로가 막히지 않는다")
    void ProviderReMatched는_같은_대상으로_다시_적재된다() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        scheduleOutboxEventCommandRepository.save(
                ScheduleOutboxEvent.create("ProviderReMatched", serviceScheduleId, "{}")
        );

        // when & then
        assertThatCode(() -> scheduleOutboxEventCommandRepository.save(
                ScheduleOutboxEvent.create("ProviderReMatched", serviceScheduleId, "{}")
        )).doesNotThrowAnyException();
    }
}
