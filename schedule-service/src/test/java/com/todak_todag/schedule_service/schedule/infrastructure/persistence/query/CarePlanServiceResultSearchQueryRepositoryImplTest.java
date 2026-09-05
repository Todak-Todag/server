package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.config.QueryDslConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, QueryDslConfig.class, CarePlanServiceResultQueryRepositoryImpl.class})
@DisplayName("서비스 수행 결과 목록 조회 - Repository (p_service_schedules 조인)")
class CarePlanServiceResultSearchQueryRepositoryImplTest {

    @Autowired
    private CarePlanServiceResultQueryRepository carePlanServiceResultQueryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("servicePreferenceIds를 전달하면 조인된 일정의 소유자가 일치하는 결과만 반환한다 (퇴원 예정자 소유권 필터링)")
    void search_withServicePreferenceIds_returnsOnlyMatchingOwner() {
        // given
        UUID myPreferenceId = UUID.randomUUID();
        UUID otherPreferenceId = UUID.randomUUID();

        CarePlanServiceResult mine = persistResult(persistSchedule(myPreferenceId, UUID.randomUUID()));
        CarePlanServiceResult others = persistResult(persistSchedule(otherPreferenceId, UUID.randomUUID()));

        entityManager.flush();
        entityManager.clear();

        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                List.of(myPreferenceId), null, PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).extracting(CarePlanServiceResult::getServiceResultId)
                .containsExactly(mine.getServiceResultId())
                .doesNotContain(others.getServiceResultId());
    }

    @Test
    @DisplayName("serviceOfferingIds를 전달하면 조인된 일정의 소유자가 일치하는 결과만 반환한다 (서비스 제공자 소유권 필터링)")
    void search_withServiceOfferingIds_returnsOnlyMatchingOwner() {
        // given
        UUID myOfferingId = UUID.randomUUID();
        UUID otherOfferingId = UUID.randomUUID();

        CarePlanServiceResult mine = persistResult(persistSchedule(UUID.randomUUID(), myOfferingId));
        CarePlanServiceResult others = persistResult(persistSchedule(UUID.randomUUID(), otherOfferingId));

        entityManager.flush();
        entityManager.clear();

        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                null, List.of(myOfferingId), PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).extracting(CarePlanServiceResult::getServiceResultId)
                .containsExactly(mine.getServiceResultId())
                .doesNotContain(others.getServiceResultId());
    }

    @Test
    @DisplayName("Internal API가 반환한 ID 목록에 없는 다른 사용자의 결과는 포함되지 않는다 (소유권 필터링 검증)")
    void search_excludesResultsOfOtherOwners() {
        // given — 내 것 1건, 남의 것 2건
        UUID myOfferingId = UUID.randomUUID();

        CarePlanServiceResult mine = persistResult(persistSchedule(UUID.randomUUID(), myOfferingId));
        CarePlanServiceResult otherA = persistResult(persistSchedule(UUID.randomUUID(), UUID.randomUUID()));
        CarePlanServiceResult otherB = persistResult(persistSchedule(UUID.randomUUID(), UUID.randomUUID()));

        entityManager.flush();
        entityManager.clear();

        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                null, List.of(myOfferingId), PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(CarePlanServiceResult::getServiceResultId)
                .containsExactly(mine.getServiceResultId())
                .doesNotContain(otherA.getServiceResultId(), otherB.getServiceResultId());
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 페이지와 올바른 pageInfo를 반환한다")
    void search_noResult_returnsEmptyPage() {
        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                null, List.of(UUID.randomUUID()), PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("페이지네이션이 정상 동작한다 (page/size)")
    void search_pagination_works() {
        // given
        UUID offeringId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            persistResult(persistSchedule(UUID.randomUUID(), offeringId));
        }
        entityManager.flush();
        entityManager.clear();

        // when
        Page<CarePlanServiceResult> firstPage = carePlanServiceResultQueryRepository.search(
                null, List.of(offeringId), PageRequest.of(0, 2)
        );
        Page<CarePlanServiceResult> secondPage = carePlanServiceResultQueryRepository.search(
                null, List.of(offeringId), PageRequest.of(1, 2)
        );

        // then
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("정렬(최신순 DESC)이 정상 동작한다")
    void search_sortDescending_ordersByCreatedAtDesc() throws InterruptedException {
        // given
        UUID offeringId = UUID.randomUUID();

        CarePlanServiceResult first = persistResult(persistSchedule(UUID.randomUUID(), offeringId));
        entityManager.flush();
        Thread.sleep(5);
        CarePlanServiceResult second = persistResult(persistSchedule(UUID.randomUUID(), offeringId));
        entityManager.flush();
        entityManager.clear();

        Pageable descending = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                null, List.of(offeringId), descending
        );

        // then
        assertThat(result.getContent()).extracting(CarePlanServiceResult::getServiceResultId)
                .containsExactly(second.getServiceResultId(), first.getServiceResultId());
    }

    @Test
    @DisplayName("정렬(오래된순 ASC)이 정상 동작한다")
    void search_sortAscending_ordersByCreatedAtAsc() throws InterruptedException {
        // given
        UUID offeringId = UUID.randomUUID();

        CarePlanServiceResult first = persistResult(persistSchedule(UUID.randomUUID(), offeringId));
        entityManager.flush();
        Thread.sleep(5);
        CarePlanServiceResult second = persistResult(persistSchedule(UUID.randomUUID(), offeringId));
        entityManager.flush();
        entityManager.clear();

        Pageable ascending = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                null, List.of(offeringId), ascending
        );

        // then
        assertThat(result.getContent()).extracting(CarePlanServiceResult::getServiceResultId)
                .containsExactly(first.getServiceResultId(), second.getServiceResultId());
    }

    @Test
    @DisplayName("소프트 삭제된 수행 결과는 목록 조회에서 제외된다")
    void search_excludesSoftDeletedResults() {
        // given
        UUID offeringId = UUID.randomUUID();

        CarePlanServiceResult active = persistResult(persistSchedule(UUID.randomUUID(), offeringId));
        CarePlanServiceResult deleted = persistResult(persistSchedule(UUID.randomUUID(), offeringId));
        deleted.markDeleted(SystemId.SYSTEM_USER_ID);

        entityManager.flush();
        entityManager.clear();

        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                null, List.of(offeringId), PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).extracting(CarePlanServiceResult::getServiceResultId)
                .containsExactly(active.getServiceResultId())
                .doesNotContain(deleted.getServiceResultId());
    }

    @Test
    @DisplayName("조인 대상 일정이 소프트 삭제되면 그 결과도 목록 조회에서 제외된다 (01번 search와 동일 정책)")
    void search_excludesResultsOfSoftDeletedSchedules() {
        // given
        UUID offeringId = UUID.randomUUID();

        CarePlanServiceResult active = persistResult(persistSchedule(UUID.randomUUID(), offeringId));

        ServiceSchedule deletedSchedule = persistSchedule(UUID.randomUUID(), offeringId);
        CarePlanServiceResult resultOfDeletedSchedule = persistResult(deletedSchedule);
        deletedSchedule.markDeleted(SystemId.SYSTEM_USER_ID);

        entityManager.flush();
        entityManager.clear();

        // when
        Page<CarePlanServiceResult> result = carePlanServiceResultQueryRepository.search(
                null, List.of(offeringId), PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).extracting(CarePlanServiceResult::getServiceResultId)
                .containsExactly(active.getServiceResultId())
                .doesNotContain(resultOfDeletedSchedule.getServiceResultId());
    }

    private ServiceSchedule persistSchedule(UUID servicePreferenceId, UUID serviceOfferingId) {
        LocalDate date = LocalDate.now().plusDays(1);
        ServiceSchedule schedule = ServiceSchedule.confirm(
                servicePreferenceId,
                serviceOfferingId,
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
