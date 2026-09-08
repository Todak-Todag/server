package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceMatchingAttemptQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceMatchingAttemptQueryServiceTest {

    @Mock
    private ServiceMatchingAttemptQueryRepository serviceMatchingAttemptQueryRepository;

    @InjectMocks
    private ServiceMatchingAttemptQueryService serviceMatchingAttemptQueryService;

    @Test
    void search은_Repository_조회_결과를_Result로_매핑해_반환한다() {
        // given
        UUID servicePreferenceId = UUID.randomUUID();
        UUID provideServiceId = UUID.randomUUID();
        List<UUID> servicePreferenceIds = List.of(servicePreferenceId);
        LocalDate date = LocalDate.of(2026, 9, 1);
        Instant failedAt = Instant.parse("2026-09-01T09:00:00Z");
        Pageable pageable = PageRequest.of(0, 10);

        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                provideServiceId,
                servicePreferenceId,
                null,
                date,
                PreferredTimeSlot.MORNING,
                MatchingAttemptStatus.FAILED,
                "해당 날짜/시간대에 제공 가능한 서비스 제공자 없음",
                null,
                failedAt
        );

        when(serviceMatchingAttemptQueryRepository.search(servicePreferenceIds, MatchingAttemptStatus.FAILED, true, pageable))
                .thenReturn(new PageImpl<>(List.of(attempt), pageable, 1));

        // when
        Page<MatchingAttemptSearchResult> result = serviceMatchingAttemptQueryService.search(
                servicePreferenceIds, MatchingAttemptStatus.FAILED, true, pageable
        );

        // then
        assertThat(result.getContent()).hasSize(1);

        MatchingAttemptSearchResult first = result.getContent().getFirst();
        assertThat(first.servicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(first.provideServiceId()).isEqualTo(provideServiceId);
        assertThat(first.date()).isEqualTo(date);
        assertThat(first.preferredTimeSlot()).isEqualTo(PreferredTimeSlot.MORNING);
        assertThat(first.status()).isEqualTo(MatchingAttemptStatus.FAILED);
        assertThat(first.failureReason()).isEqualTo("해당 날짜/시간대에 제공 가능한 서비스 제공자 없음");
        assertThat(first.failedAt()).isEqualTo(failedAt);
        assertThat(first.matchedAt()).isNull();

        verify(serviceMatchingAttemptQueryRepository)
                .search(servicePreferenceIds, MatchingAttemptStatus.FAILED, true, pageable);
    }

    @Test
    void 조회_결과가_없으면_빈_페이지를_반환한다() {
        // given
        List<UUID> servicePreferenceIds = List.of(UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 10);

        when(serviceMatchingAttemptQueryRepository.search(servicePreferenceIds, MatchingAttemptStatus.FAILED, true, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<MatchingAttemptSearchResult> result = serviceMatchingAttemptQueryService.search(
                servicePreferenceIds, MatchingAttemptStatus.FAILED, true, pageable
        );

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
