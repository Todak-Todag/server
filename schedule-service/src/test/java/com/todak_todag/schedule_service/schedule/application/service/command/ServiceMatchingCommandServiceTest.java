package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchedEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceMatchingCommandServiceTest {

    @Mock
    private ServiceScheduleCommandRepository serviceScheduleCommandRepository;

    @Mock
    private ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;

    @InjectMocks
    private ServiceMatchingCommandService serviceMatchingCommandService;

    @Test
    @DisplayName("신규 매칭이면 새 일정이 SCHEDULED로 생성된다")
    void 신규_매칭이면_일정이_생성된다() {
        // given
        ProviderMatchedEvent event = matchedEvent(LocalDate.now().plusDays(3));

        when(serviceMatchingAttemptCommandRepository.existsMatched(any(), any(), any(), any())).thenReturn(false);
        when(serviceScheduleCommandRepository.findRescheduling(event.servicePreferenceId())).thenReturn(List.of());

        // when
        serviceMatchingCommandService.applyMatched(event);

        // then
        ServiceSchedule saved = capturedSchedule();

        assertThat(saved.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(saved.getCarePlanId()).isEqualTo(event.carePlanId());
        assertThat(saved.getServicePreferenceId()).isEqualTo(event.servicePreferenceId());
        assertThat(saved.getServiceOfferingId()).isEqualTo(event.serviceOfferingId());
        assertThat(saved.getDate()).isEqualTo(event.date());
        assertThat(saved.getStartedAt()).isEqualTo(event.startedAt());
    }

    @Test
    @DisplayName("페이로드에 없는 finishedAt은 startedAt + 고정 소요시간(1시간)으로 채워진다")
    void finishedAt은_고정_소요시간으로_채워진다() {
        // given
        ProviderMatchedEvent event = matchedEvent(LocalDate.now().plusDays(3));

        when(serviceMatchingAttemptCommandRepository.existsMatched(any(), any(), any(), any())).thenReturn(false);
        when(serviceScheduleCommandRepository.findRescheduling(event.servicePreferenceId())).thenReturn(List.of());

        // when
        serviceMatchingCommandService.applyMatched(event);

        // then
        ServiceSchedule saved = capturedSchedule();

        assertThat(saved.getFinishedAt())
                .isEqualTo(event.startedAt().plus(ServiceMatchingCommandService.DEFAULT_SERVICE_DURATION));
        assertThat(saved.getFinishedAt()).isEqualTo(event.startedAt().plusHours(1));
    }

    @Test
    @DisplayName("재매칭이면 기존 RESCHEDULING 일정이 CHANGED가 되고 새 일정이 따로 생성된다")
    void 재매칭이면_기존_일정이_변경완료되고_새_일정이_생긴다() {
        // given
        UUID servicePreferenceId = UUID.randomUUID();
        ServiceSchedule existing = reschedulingSchedule(servicePreferenceId, LocalDate.now().plusDays(3));

        LocalDate rematchedDate = existing.getDate().plusDays(1);
        ProviderMatchedEvent event = matchedEvent(rematchedDate, servicePreferenceId);

        when(serviceMatchingAttemptCommandRepository.existsMatched(any(), any(), any(), any())).thenReturn(false);
        when(serviceScheduleCommandRepository.findRescheduling(servicePreferenceId)).thenReturn(List.of(existing));

        // when
        serviceMatchingCommandService.applyMatched(event);

        // then
        assertThat(existing.getStatus()).isEqualTo(ScheduleStatus.CHANGED);
        assertThat(existing.getDate()).isNotEqualTo(rematchedDate);

        ServiceSchedule saved = capturedSchedule();

        assertThat(saved).isNotSameAs(existing);
        assertThat(saved.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(saved.getDate()).isEqualTo(rematchedDate);
        assertThat(saved.getServiceOfferingId()).isEqualTo(event.serviceOfferingId());
    }

    @Test
    @DisplayName("매칭 시도 결과가 페이로드 기준으로 기록되며 preferredTimeSlot은 null이다")
    void 매칭_시도_결과가_기록된다() {
        // given
        ProviderMatchedEvent event = matchedEvent(LocalDate.now().plusDays(3));

        when(serviceMatchingAttemptCommandRepository.existsMatched(any(), any(), any(), any())).thenReturn(false);
        when(serviceScheduleCommandRepository.findRescheduling(event.servicePreferenceId())).thenReturn(List.of());

        // when
        serviceMatchingCommandService.applyMatched(event);

        // then
        ArgumentCaptor<ServiceMatchingAttempt> captor = ArgumentCaptor.forClass(ServiceMatchingAttempt.class);
        verify(serviceMatchingAttemptCommandRepository).save(captor.capture());

        ServiceMatchingAttempt attempt = captor.getValue();

        assertThat(attempt.getCarePlanId()).isEqualTo(event.carePlanId());
        assertThat(attempt.getRegionId()).isEqualTo(event.regionId());
        assertThat(attempt.getProvideServiceId()).isEqualTo(event.provideServiceId());
        assertThat(attempt.getServicePreferenceId()).isEqualTo(event.servicePreferenceId());
        assertThat(attempt.getServiceOfferingId()).isEqualTo(event.serviceOfferingId());
        assertThat(attempt.getDate()).isEqualTo(event.date());
        assertThat(attempt.getStatus()).isEqualTo(MatchingAttemptStatus.MATCHED);
        assertThat(attempt.getMatchedAt()).isEqualTo(event.matchedAt());

        assertThat(attempt.getPreferredTimeSlot()).isNull();
        assertThat(attempt.getFailureReason()).isNull();
        assertThat(attempt.getFailedAt()).isNull();
    }

    @Test
    @DisplayName("이미 처리한 이벤트를 다시 받으면 아무것도 저장하지 않는다")
    void 중복_수신은_처리되지_않는다() {
        // given
        ProviderMatchedEvent event = matchedEvent(LocalDate.now().plusDays(3));

        when(serviceMatchingAttemptCommandRepository.existsMatched(
                event.servicePreferenceId(),
                event.serviceOfferingId(),
                event.date(),
                event.matchedAt()
        )).thenReturn(true);

        // when
        serviceMatchingCommandService.applyMatched(event);

        // then
        verify(serviceMatchingAttemptCommandRepository, never()).save(any());
        verify(serviceScheduleCommandRepository, never()).save(any());
        verify(serviceScheduleCommandRepository, never()).findRescheduling(any());
    }

    @Test
    @DisplayName("RESCHEDULING 일정이 둘 이상이면 어느 것을 닫을지 정할 수 없어 실패한다")
    void 변경중_일정이_둘_이상이면_실패한다() {
        // given
        UUID servicePreferenceId = UUID.randomUUID();
        ProviderMatchedEvent event = matchedEvent(LocalDate.now().plusDays(3), servicePreferenceId);

        when(serviceMatchingAttemptCommandRepository.existsMatched(any(), any(), any(), any())).thenReturn(false);
        when(serviceScheduleCommandRepository.findRescheduling(servicePreferenceId)).thenReturn(List.of(
                reschedulingSchedule(servicePreferenceId, LocalDate.now().plusDays(3)),
                reschedulingSchedule(servicePreferenceId, LocalDate.now().plusDays(4))
        ));

        // when & then
        assertThatThrownBy(() -> serviceMatchingCommandService.applyMatched(event))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_MULTIPLE_RESCHEDULING);

        verify(serviceScheduleCommandRepository, never()).save(any());
    }

    private ServiceSchedule capturedSchedule() {
        ArgumentCaptor<ServiceSchedule> captor = ArgumentCaptor.forClass(ServiceSchedule.class);
        verify(serviceScheduleCommandRepository).save(captor.capture());

        return captor.getValue();
    }

    private ProviderMatchedEvent matchedEvent(LocalDate date) {
        return matchedEvent(date, UUID.randomUUID());
    }

    // ProviderMatched 이벤트
    private ProviderMatchedEvent matchedEvent(LocalDate date, UUID servicePreferenceId) {
        return new ProviderMatchedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                date.atTime(10, 0),
                Instant.parse("2026-08-29T10:00:00Z")
        );
    }

    // RESCHEDULING 상태가 된 기존 일정
    private ServiceSchedule reschedulingSchedule(UUID servicePreferenceId, LocalDate date) {
        ServiceSchedule schedule = ServiceSchedule.confirm(
                UUID.randomUUID(),
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );

        schedule.rescheduling();

        return schedule;
    }
}
