package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.config.QueryDslConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, QueryDslConfig.class, ServiceScheduleQueryRepositoryImpl.class})
class ServiceScheduleQueryRepositoryImplTest {

    @Autowired
    private ServiceScheduleQueryRepository serviceScheduleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void SCHEDULED와_RESCHEDULING_상태만_반환하고_나머지_상태는_제외한다() {
        // given
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);

        ServiceSchedule scheduled = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        ServiceSchedule rescheduling = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        rescheduling.rescheduling();

        ServiceSchedule changed = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        setStatus(changed, ScheduleStatus.CHANGED);

        ServiceSchedule completed = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        completed.complete();

        ServiceSchedule noShow = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        noShow.markNoShow();

        ServiceSchedule canceled = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        canceled.cancel("사유");

        entityManager.flush();
        entityManager.clear();

        // when
        List<ServiceSchedule> result = serviceScheduleRepository.findSchedules(
                List.of(serviceOfferingId), startDate, startDate.plusDays(29),
                List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING)
        );

        // then
        assertThat(result).extracting(ServiceSchedule::getStatus)
                .containsExactlyInAnyOrder(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING);
    }

    @Test
    void 여러_serviceOfferingId를_동시에_조회하면_모두_반환한다() {
        // given
        UUID offeringA = UUID.randomUUID();
        UUID offeringB = UUID.randomUUID();
        UUID offeringC = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);

        persistSchedule(UUID.randomUUID(), offeringA, startDate);
        persistSchedule(UUID.randomUUID(), offeringB, startDate);
        persistSchedule(UUID.randomUUID(), offeringC, startDate);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ServiceSchedule> result = serviceScheduleRepository.findSchedules(
                List.of(offeringA, offeringB), startDate, startDate.plusDays(29),
                List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING)
        );

        // then
        assertThat(result).extracting(ServiceSchedule::getServiceOfferingId)
                .containsExactlyInAnyOrder(offeringA, offeringB);
    }

    @Test
    void 조회_결과가_없으면_빈_리스트를_반환한다() {
        // given
        LocalDate startDate = LocalDate.of(2026, 9, 1);

        // when
        List<ServiceSchedule> result = serviceScheduleRepository.findSchedules(
                List.of(UUID.randomUUID()), startDate, startDate.plusDays(29),
                List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 조회_범위는_startDate부터_29일_뒤까지이며_경계값을_포함한다() {
        // given
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = startDate.plusDays(29);

        ServiceSchedule beforeRange = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate.minusDays(1));
        ServiceSchedule onStartBoundary = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        ServiceSchedule onEndBoundary = persistSchedule(UUID.randomUUID(), serviceOfferingId, endDate);
        ServiceSchedule afterRange = persistSchedule(UUID.randomUUID(), serviceOfferingId, endDate.plusDays(1));

        entityManager.flush();
        entityManager.clear();

        // when
        List<ServiceSchedule> result = serviceScheduleRepository.findSchedules(
                List.of(serviceOfferingId), startDate, endDate,
                List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING)
        );

        // then
        assertThat(result).extracting(ServiceSchedule::getId)
                .containsExactlyInAnyOrder(onStartBoundary.getId(), onEndBoundary.getId())
                .doesNotContain(beforeRange.getId(), afterRange.getId());
    }

    @Test
    void 소프트_삭제된_일정은_결과에서_제외된다() {
        // given
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);

        ServiceSchedule active = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        ServiceSchedule deleted = persistSchedule(UUID.randomUUID(), serviceOfferingId, startDate);
        deleted.markDeleted(SystemId.SYSTEM_USER_ID);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ServiceSchedule> result = serviceScheduleRepository.findSchedules(
                List.of(serviceOfferingId), startDate, startDate.plusDays(29),
                List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING)
        );

        // then
        assertThat(result).extracting(ServiceSchedule::getId)
                .containsExactly(active.getId())
                .doesNotContain(deleted.getId());
    }

    @Test
    void 인증_주체가_없으면_created_by에_SYSTEM_USER_ID가_기록된다() {
        // given & when
        ServiceSchedule schedule = persistSchedule(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1));
        entityManager.flush();
        entityManager.clear();

        // then
        ServiceSchedule found = entityManager.find(ServiceSchedule.class, schedule.getId());
        assertThat(found.getCreatedBy()).isEqualTo(SystemId.SYSTEM_USER_ID);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void id로_조회하면_해당_일정을_반환한다() {
        // given
        ServiceSchedule schedule = persistSchedule(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1));
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<ServiceSchedule> result = serviceScheduleRepository.findById(schedule.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(schedule.getId());
    }

    @Test
    void 존재하지_않는_id로_조회하면_빈_Optional을_반환한다() {
        // when
        Optional<ServiceSchedule> result = serviceScheduleRepository.findById(UUID.randomUUID());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 소프트_삭제된_일정은_id로_조회해도_반환되지_않는다() {
        // given
        ServiceSchedule schedule = persistSchedule(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1));
        schedule.markDeleted(SystemId.SYSTEM_USER_ID);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<ServiceSchedule> result = serviceScheduleRepository.findById(schedule.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Nested
    @DisplayName("서비스 일정 목록 조회")
    class searchTest {

        @Test
        @DisplayName("servicePreferenceIds를 전달하면 해당 ID 소유의 일정만 반환한다 (퇴원 예정자 소유권 필터링)")
        void search_withServicePreferenceIds_returnsOnlyMatchingOwner() {
            // given
            UUID myPreferenceId = UUID.randomUUID();
            UUID otherPreferenceId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule mine = persistSchedule(myPreferenceId, UUID.randomUUID(), date);
            ServiceSchedule others = persistSchedule(otherPreferenceId, UUID.randomUUID(), date);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    List.of(myPreferenceId), null, null, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .containsExactly(mine.getId())
                    .doesNotContain(others.getId());
        }

        @Test
        @DisplayName("serviceOfferingIds를 전달하면 해당 ID 소유의 일정만 반환한다 (서비스 제공자 소유권 필터링)")
        void search_withServiceOfferingIds_returnsOnlyMatchingOwner() {
            // given
            UUID myOfferingId = UUID.randomUUID();
            UUID otherOfferingId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule mine = persistSchedule(UUID.randomUUID(), myOfferingId, date);
            ServiceSchedule others = persistSchedule(UUID.randomUUID(), otherOfferingId, date);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(myOfferingId), null, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .containsExactly(mine.getId())
                    .doesNotContain(others.getId());
        }

        @Test
        @DisplayName("status 필터를 전달하면 해당 상태의 일정만 반환한다")
        void search_withStatusFilter_returnsOnlyMatchingStatus() {
            // given
            UUID offeringId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule scheduled = persistSchedule(UUID.randomUUID(), offeringId, date);
            ServiceSchedule canceled = persistSchedule(UUID.randomUUID(), offeringId, date);
            canceled.cancel("사유");

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(offeringId), ScheduleStatus.SCHEDULED, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .containsExactly(scheduled.getId())
                    .doesNotContain(canceled.getId());
        }

        @Test
        @DisplayName("date 필터를 전달하면 해당 날짜의 일정만 반환한다")
        void search_withDateFilter_returnsOnlyMatchingDate() {
            // given
            UUID offeringId = UUID.randomUUID();
            LocalDate targetDate = LocalDate.now().plusDays(1);
            LocalDate otherDate = LocalDate.now().plusDays(2);

            ServiceSchedule onTargetDate = persistSchedule(UUID.randomUUID(), offeringId, targetDate);
            ServiceSchedule onOtherDate = persistSchedule(UUID.randomUUID(), offeringId, otherDate);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(offeringId), null, targetDate, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .containsExactly(onTargetDate.getId())
                    .doesNotContain(onOtherDate.getId());
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 페이지와 올바른 pageInfo를 반환한다")
        void search_noResult_returnsEmptyPage() {
            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(UUID.randomUUID()), null, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
        }

        @Test
        @DisplayName("servicePreferenceIds가 null이 아닌 빈 리스트면 소유권 필터가 그대로 적용되어 다른 소유자의 일정이 결과에 포함되지 않는다")
        void search_emptyServicePreferenceIds_stillAppliesOwnershipFilter() {
            // given
            UUID otherPreferenceId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule others = persistSchedule(otherPreferenceId, UUID.randomUUID(), date);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    List.of(), null, null, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .doesNotContain(others.getId());
        }

        @Test
        @DisplayName("serviceOfferingIds가 null이 아닌 빈 리스트면 소유권 필터가 그대로 적용되어 다른 소유자의 일정이 결과에 포함되지 않는다")
        void search_emptyServiceOfferingIds_stillAppliesOwnershipFilter() {
            // given
            UUID otherOfferingId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule others = persistSchedule(UUID.randomUUID(), otherOfferingId, date);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(), null, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .doesNotContain(others.getId());
        }

        @Test
        @DisplayName("페이지네이션이 정상 동작한다 (page/size)")
        void search_pagination_works() {
            // given
            UUID offeringId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            for (int i = 0; i < 3; i++) {
                persistSchedule(UUID.randomUUID(), offeringId, date);
            }
            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> firstPage = serviceScheduleRepository.search(
                    null, List.of(offeringId), null, null, PageRequest.of(0, 2)
            );
            Page<ServiceSchedule> secondPage = serviceScheduleRepository.search(
                    null, List.of(offeringId), null, null, PageRequest.of(1, 2)
            );

            // then
            assertThat(firstPage.getContent()).hasSize(2);
            assertThat(firstPage.getTotalElements()).isEqualTo(3);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);
            assertThat(secondPage.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("정렬(오래된순 ASC)이 정상 동작한다")
        void search_sortAscending_ordersByCreatedAtAsc() throws InterruptedException {
            // given
            UUID offeringId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule first = persistSchedule(UUID.randomUUID(), offeringId, date);
            entityManager.flush();
            Thread.sleep(5);
            ServiceSchedule second = persistSchedule(UUID.randomUUID(), offeringId, date);
            entityManager.flush();
            entityManager.clear();

            Pageable ascending = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(offeringId), null, null, ascending
            );

            // then
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .containsExactly(first.getId(), second.getId());
        }

        @Test
        @DisplayName("정렬(최신순 DESC)이 정상 동작한다")
        void search_sortDescending_ordersByCreatedAtDesc() throws InterruptedException {
            // given
            UUID offeringId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule first = persistSchedule(UUID.randomUUID(), offeringId, date);
            entityManager.flush();
            Thread.sleep(5);
            ServiceSchedule second = persistSchedule(UUID.randomUUID(), offeringId, date);
            entityManager.flush();
            entityManager.clear();

            Pageable descending = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(offeringId), null, null, descending
            );

            // then
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .containsExactly(second.getId(), first.getId());
        }

        @Test
        @DisplayName("소프트 삭제된 일정은 목록 조회에서 제외된다")
        void search_excludesSoftDeletedSchedules() {
            // given
            UUID offeringId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceSchedule active = persistSchedule(UUID.randomUUID(), offeringId, date);
            ServiceSchedule deleted = persistSchedule(UUID.randomUUID(), offeringId, date);
            deleted.markDeleted(SystemId.SYSTEM_USER_ID);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceSchedule> result = serviceScheduleRepository.search(
                    null, List.of(offeringId), null, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).extracting(ServiceSchedule::getId)
                    .containsExactly(active.getId())
                    .doesNotContain(deleted.getId());
        }
    }

    private ServiceSchedule persistSchedule(UUID servicePreferenceId, UUID serviceOfferingId, LocalDate date) {
        ServiceSchedule schedule = ServiceSchedule.confirm(
                servicePreferenceId,
                serviceOfferingId,
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
        return entityManager.persist(schedule);
    }

    private void setStatus(ServiceSchedule schedule, ScheduleStatus status) {
        try {
            Field statusField = ServiceSchedule.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(schedule, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
