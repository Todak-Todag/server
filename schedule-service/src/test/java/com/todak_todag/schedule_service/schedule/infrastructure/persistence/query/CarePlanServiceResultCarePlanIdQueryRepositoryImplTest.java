package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.config.QueryDslConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, QueryDslConfig.class, CarePlanServiceResultQueryRepositoryImpl.class})
@DisplayName("[내부 API] 수행 결과의 케어플랜 ID 조회 - Repository (p_service_schedules 조인)")
class CarePlanServiceResultCarePlanIdQueryRepositoryImplTest extends PostgresTestSupport {

    @Autowired
    private CarePlanServiceResultQueryRepository carePlanServiceResultQueryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("결과와 조인 대상 일정이 모두 유효하면 일정의 carePlanId를 반환한다")
    void findCarePlanId_returnsCarePlanIdOfJoinedSchedule() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule schedule = persistSchedule(carePlanId);
        CarePlanServiceResult result = persistResult(schedule);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<UUID> found = carePlanServiceResultQueryRepository
                .findCarePlanIdByServiceResultId(result.getServiceResultId());

        // then
        assertThat(found).contains(carePlanId);
    }

    @Test
    @DisplayName("다른 결과가 함께 존재해도 조회 대상 결과가 속한 케어플랜 ID만 반환한다")
    void findCarePlanId_returnsOnlyOwnCarePlanId() {
        // given
        UUID targetCarePlanId = UUID.randomUUID();
        CarePlanServiceResult target = persistResult(persistSchedule(targetCarePlanId));

        UUID otherCarePlanId = UUID.randomUUID();
        persistResult(persistSchedule(otherCarePlanId));

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<UUID> found = carePlanServiceResultQueryRepository
                .findCarePlanIdByServiceResultId(target.getServiceResultId());

        // then
        assertThat(found).contains(targetCarePlanId);
        assertThat(found).get().isNotEqualTo(otherCarePlanId);
    }

    @Test
    @DisplayName("존재하지 않는 serviceResultId면 빈 Optional을 반환한다")
    void findCarePlanId_notExistingResult_returnsEmpty() {
        // when
        Optional<UUID> found = carePlanServiceResultQueryRepository
                .findCarePlanIdByServiceResultId(UUID.randomUUID());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("소프트 삭제된 수행 결과는 빈 Optional을 반환한다")
    void findCarePlanId_softDeletedResult_returnsEmpty() {
        // given
        CarePlanServiceResult result = persistResult(persistSchedule(UUID.randomUUID()));
        result.markDeleted(SystemId.SYSTEM_USER_ID);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<UUID> found = carePlanServiceResultQueryRepository
                .findCarePlanIdByServiceResultId(result.getServiceResultId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("조인 대상 일정이 소프트 삭제되면 빈 Optional을 반환한다")
    void findCarePlanId_softDeletedSchedule_returnsEmpty() {
        // given
        ServiceSchedule schedule = persistSchedule(UUID.randomUUID());
        CarePlanServiceResult result = persistResult(schedule);
        schedule.markDeleted(SystemId.SYSTEM_USER_ID);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<UUID> found = carePlanServiceResultQueryRepository
                .findCarePlanIdByServiceResultId(result.getServiceResultId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("조인 대상 일정이 아예 없으면 빈 Optional을 반환한다")
    void findCarePlanId_scheduleNotExisting_returnsEmpty() {
        // given
        CarePlanServiceResult orphan = CarePlanServiceResult.record(
                UUID.randomUUID(),
                LocalDate.now().plusDays(1).atTime(9, 0),
                LocalDate.now().plusDays(1).atTime(10, 0),
                "정상 수행 완료"
        );
        entityManager.persist(orphan);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<UUID> found = carePlanServiceResultQueryRepository
                .findCarePlanIdByServiceResultId(orphan.getServiceResultId());

        // then
        assertThat(found).isEmpty();
    }

    private ServiceSchedule persistSchedule(UUID carePlanId) {
        LocalDate date = LocalDate.now().plusDays(1);
        ServiceSchedule schedule = ServiceSchedule.confirm(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
        return entityManager.persist(schedule);
    }

    private CarePlanServiceResult persistResult(ServiceSchedule schedule) {
        CarePlanServiceResult result = CarePlanServiceResult.record(
                schedule.getId(),
                schedule.getStartedAt(),
                schedule.getFinishedAt(),
                "정상 수행 완료"
        );
        return entityManager.persist(result);
    }
}
