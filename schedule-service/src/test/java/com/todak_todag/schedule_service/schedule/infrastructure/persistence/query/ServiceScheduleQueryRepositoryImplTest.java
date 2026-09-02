package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.config.QueryDslConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
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
