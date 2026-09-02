package com.todak_todag.schedule_service.schedule.application.query_service;

import com.todak_todag.schedule_service.schedule.application.query.InternalServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.InternalServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalServiceScheduleQueryServiceTest {

    @Mock
    private ServiceScheduleQueryRepository serviceScheduleRepository;

    @InjectMocks
    private InternalServiceScheduleQueryService internalServiceScheduleQueryService;

    @Test
    void startDate로부터_29일_뒤까지를_조회_범위_종료일로_전달한다() {
        // given
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate expectedEndDate = LocalDate.of(2026, 9, 30);

        when(serviceScheduleRepository.findSchedules(any(), any(), any(), any()))
                .thenReturn(List.of());

        // when
        internalServiceScheduleQueryService.search(new InternalServiceScheduleSearchQuery(List.of(serviceOfferingId), startDate));

        // then
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(serviceScheduleRepository).findSchedules(
                eq(List.of(serviceOfferingId)), startCaptor.capture(), endCaptor.capture(), eq(List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING))
        );
        assertThat(startCaptor.getValue()).isEqualTo(startDate);
        assertThat(endCaptor.getValue()).isEqualTo(expectedEndDate);
    }

    @Test
    void SCHEDULED와_RESCHEDULING_상태만_조회_대상으로_전달한다() {
        // given
        when(serviceScheduleRepository.findSchedules(any(), any(), any(), any()))
                .thenReturn(List.of());

        // when
        internalServiceScheduleQueryService.search(new InternalServiceScheduleSearchQuery(List.of(UUID.randomUUID()), LocalDate.now()));

        // then
        ArgumentCaptor<List<ScheduleStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(serviceScheduleRepository).findSchedules(any(), any(), any(), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING);
    }

    @Test
    void 여러_serviceOfferingIds를_전달하면_그대로_조회_조건에_사용한다() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(serviceScheduleRepository.findSchedules(any(), any(), any(), any()))
                .thenReturn(List.of());

        // when
        internalServiceScheduleQueryService.search(new InternalServiceScheduleSearchQuery(List.of(id1, id2), LocalDate.now()));

        // then
        verify(serviceScheduleRepository).findSchedules(
                eq(List.of(id1, id2)), any(), any(), any()
        );
    }

    @Test
    void 조회_결과가_없으면_빈_리스트를_반환한다() {
        // given
        when(serviceScheduleRepository.findSchedules(any(), any(), any(), any()))
                .thenReturn(List.of());

        // when
        List<InternalServiceScheduleSearchResult> results = internalServiceScheduleQueryService.search(
                new InternalServiceScheduleSearchQuery(List.of(UUID.randomUUID()), LocalDate.now())
        );

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void 조회된_엔티티를_Result로_매핑한다() throws Exception {
        // given
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        LocalDateTime startedAt = date.atTime(9, 0);
        LocalDateTime finishedAt = date.atTime(10, 0);

        ServiceSchedule schedule = ServiceSchedule.confirm(UUID.randomUUID(), serviceOfferingId, date, startedAt, finishedAt);
        UUID scheduleId = UUID.randomUUID();
        setId(schedule, scheduleId);

        when(serviceScheduleRepository.findSchedules(any(), any(), any(), any()))
                .thenReturn(List.of(schedule));

        // when
        List<InternalServiceScheduleSearchResult> results = internalServiceScheduleQueryService.search(
                new InternalServiceScheduleSearchQuery(List.of(serviceOfferingId), date)
        );

        // then
        assertThat(results).hasSize(1);
        InternalServiceScheduleSearchResult result = results.get(0);
        assertThat(result.serviceScheduleId()).isEqualTo(scheduleId);
        assertThat(result.serviceOfferingId()).isEqualTo(serviceOfferingId);
        assertThat(result.date()).isEqualTo(date);
        assertThat(result.startedAt()).isEqualTo(startedAt);
        assertThat(result.finishedAt()).isEqualTo(finishedAt);
        assertThat(result.status()).isEqualTo(ScheduleStatus.SCHEDULED);
    }

    private void setId(ServiceSchedule schedule, UUID id) throws Exception {
        Field idField = ServiceSchedule.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(schedule, id);
    }
}
