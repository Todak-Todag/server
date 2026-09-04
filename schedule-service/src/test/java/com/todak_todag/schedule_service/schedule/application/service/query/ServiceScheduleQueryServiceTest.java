package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceScheduleQueryServiceTest {

    @Mock
    private ServiceScheduleQueryRepository serviceScheduleQueryRepository;

    @InjectMocks
    private ServiceScheduleQueryService serviceScheduleQueryService;

    @Test
    void search은_Repository_조회_결과를_Result로_매핑해_반환한다() {
        // given
        List<UUID> servicePreferenceIds = List.of(UUID.randomUUID());
        ScheduleStatus status = ScheduleStatus.SCHEDULED;
        LocalDate date = LocalDate.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);

        ServiceSchedule schedule = ServiceSchedule.confirm(
                servicePreferenceIds.get(0), UUID.randomUUID(), date, date.atTime(9, 0), date.atTime(10, 0)
        );

        when(serviceScheduleQueryRepository.search(servicePreferenceIds, null, status, date, pageable))
                .thenReturn(new PageImpl<>(List.of(schedule), pageable, 1));

        // when
        var result = serviceScheduleQueryService.search(servicePreferenceIds, null, status, date, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        ServiceScheduleSearchResult content = result.getContent().get(0);
        assertThat(content.serviceScheduleId()).isEqualTo(schedule.getId());
        assertThat(content.status()).isEqualTo(ScheduleStatus.SCHEDULED);
        verify(serviceScheduleQueryRepository).search(servicePreferenceIds, null, status, date, pageable);
    }

    @Test
    void search은_조회_결과가_없으면_빈_페이지를_반환한다() {
        // given
        List<UUID> serviceOfferingIds = List.of(UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 10);

        when(serviceScheduleQueryRepository.search(null, serviceOfferingIds, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        var result = serviceScheduleQueryService.search(null, serviceOfferingIds, null, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findDetailById는_Repository_조회_결과를_전체_필드를_담은_Result로_매핑해_반환한다() {
        // given
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        ServiceSchedule schedule = ServiceSchedule.confirm(
                servicePreferenceId, serviceOfferingId, date, date.atTime(9, 0), date.atTime(10, 0)
        );

        when(serviceScheduleQueryRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        // when
        Optional<ServiceScheduleDetailResult> result = serviceScheduleQueryService.findDetailById(schedule.getId());

        // then
        assertThat(result).isPresent();
        ServiceScheduleDetailResult detail = result.get();
        assertThat(detail.serviceScheduleId()).isEqualTo(schedule.getId());
        assertThat(detail.servicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(detail.serviceOfferingId()).isEqualTo(serviceOfferingId);
        assertThat(detail.status()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(detail.cancelReason()).isNull();
        assertThat(detail.canceledAt()).isNull();
    }

    @Test
    void findDetailById는_취소된_일정이면_cancelReason과_canceledAt을_함께_반환한다() {
        // given
        LocalDate date = LocalDate.now().plusDays(1);
        ServiceSchedule schedule = ServiceSchedule.confirm(
                UUID.randomUUID(), UUID.randomUUID(), date, date.atTime(9, 0), date.atTime(10, 0)
        );
        schedule.cancel("개인 사정으로 취소합니다");

        when(serviceScheduleQueryRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        // when
        Optional<ServiceScheduleDetailResult> result = serviceScheduleQueryService.findDetailById(schedule.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(ScheduleStatus.CANCELED);
        assertThat(result.get().cancelReason()).isEqualTo("개인 사정으로 취소합니다");
        assertThat(result.get().canceledAt()).isNotNull();
    }

    @Test
    void findDetailById는_존재하지_않으면_빈_Optional을_반환한다() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        when(serviceScheduleQueryRepository.findById(serviceScheduleId)).thenReturn(Optional.empty());

        // when
        Optional<ServiceScheduleDetailResult> result = serviceScheduleQueryService.findDetailById(serviceScheduleId);

        // then
        assertThat(result).isEmpty();
    }
}
