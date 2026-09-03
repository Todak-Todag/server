package com.todak_todag.schedule_service.schedule.application.service.query;

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
}
