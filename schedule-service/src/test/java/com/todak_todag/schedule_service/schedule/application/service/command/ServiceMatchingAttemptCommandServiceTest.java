package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.MatchingAttemptRetryCommand;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptRetryResult;
import com.todak_todag.schedule_service.schedule.application.support.MatchingAttemptValidator;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceMatchingAttemptCommandServiceTest {

    private static final UUID MATCHING_ATTEMPT_ID = UUID.randomUUID();
    private static final UUID CARE_PLAN_ID = UUID.randomUUID();
    private static final UUID REGION_ID = UUID.randomUUID();
    private static final UUID PROVIDE_SERVICE_ID = UUID.randomUUID();
    private static final UUID SERVICE_PREFERENCE_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    // 2026-09-01 ~ 2026-09-30 (finishDate에서 30일 고정 기간으로 역산)
    private static final LocalDate FINISH_DATE = LocalDate.of(2026, 9, 30);
    private static final LocalDate REQUESTED_DATE = LocalDate.of(2026, 9, 10);

    @Mock
    private ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;

    @Mock
    private ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;

    @Mock
    private ScheduleOutboxCommandService scheduleOutboxCommandService;

    private ServiceMatchingAttemptCommandService serviceMatchingAttemptCommandService;

    // 페이로드 검증까지 하기 위해 직렬화기와 검증기는 실제 구현을 사용
    @BeforeEach
    void setUp() {
        serviceMatchingAttemptCommandService = new ServiceMatchingAttemptCommandService(
                serviceMatchingAttemptCommandRepository,
                scheduleOutboxEventCommandRepository,
                scheduleOutboxCommandService,
                new ProviderReMatchEventPayloadSerializer(
                        // Boot가 구성하는 ObjectMapper와 같은 설정으로 맞춤
                        new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                ),
                new ServiceScheduleValidator(),
                new MatchingAttemptValidator()
        );
    }

    private ServiceMatchingAttempt attempt(MatchingAttemptStatus status) {
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                CARE_PLAN_ID,
                REGION_ID,
                PROVIDE_SERVICE_ID,
                SERVICE_PREFERENCE_ID,
                null,
                LocalDate.of(2026, 9, 1),
                PreferredTimeSlot.AFTERNOON,
                status,
                status == MatchingAttemptStatus.FAILED ? "제공 가능한 서비스 제공자 없음" : null,
                null,
                status == MatchingAttemptStatus.FAILED ? Instant.parse("2026-09-01T09:00:00Z") : null
        );

        // ID는 영속화 시점에 생성되므로 테스트에서는 직접 주입
        ReflectionTestUtils.setField(attempt, "id", MATCHING_ATTEMPT_ID);

        return attempt;
    }

    private CarePlanPort.CarePlanRange carePlanRange(UUID patientId) {
        return new CarePlanPort.CarePlanRange(CARE_PLAN_ID, FINISH_DATE, patientId);
    }

    private MatchingAttemptRetryCommand command(LocalDate date, UUID requesterId) {
        return new MatchingAttemptRetryCommand(MATCHING_ATTEMPT_ID, date, PreferredTimeSlot.MORNING, requesterId);
    }

    @Test
    @DisplayName("대상 매칭 시도가 FAILED면 ProviderReMatched가 아웃박스에 적재된다")
    void FAILED면_이벤트가_적재된다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findById(MATCHING_ATTEMPT_ID))
                .thenReturn(Optional.of(attempt(MatchingAttemptStatus.FAILED)));
        when(scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(
                ProviderReMatchEventPort.EVENT_TYPE, MATCHING_ATTEMPT_ID)
        ).thenReturn(false);

        // when
        MatchingAttemptRetryResult result = serviceMatchingAttemptCommandService.retry(
                command(REQUESTED_DATE, PATIENT_ID),
                carePlanRange(PATIENT_ID)
        );

        // then
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(scheduleOutboxCommandService).enqueue(
                eq(ProviderReMatchEventPort.EVENT_TYPE),
                eq(MATCHING_ATTEMPT_ID),
                payloadCaptor.capture()
        );

        assertThat(payloadCaptor.getValue()).isEqualTo(
                "{\"carePlanId\":\"" + CARE_PLAN_ID + "\","
                        + "\"regionId\":\"" + REGION_ID + "\","
                        + "\"provideServiceId\":\"" + PROVIDE_SERVICE_ID + "\","
                        + "\"servicePreferenceId\":\"" + SERVICE_PREFERENCE_ID + "\","
                        + "\"date\":\"" + REQUESTED_DATE + "\","
                        + "\"preferredTimeSlot\":\"MORNING\"}"
        );

        assertThat(result).isEqualTo(
                new MatchingAttemptRetryResult(
                        MATCHING_ATTEMPT_ID,
                        SERVICE_PREFERENCE_ID,
                        REQUESTED_DATE,
                        PreferredTimeSlot.MORNING
                )
        );
    }

    @Test
    @DisplayName("매칭 시도 이력을 동기적으로 새로 생성하지 않는다")
    void 매칭_시도_이력을_생성하지_않는다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findById(MATCHING_ATTEMPT_ID))
                .thenReturn(Optional.of(attempt(MatchingAttemptStatus.FAILED)));
        when(scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(anyString(), any()))
                .thenReturn(false);

        // when
        serviceMatchingAttemptCommandService.retry(command(REQUESTED_DATE, PATIENT_ID), carePlanRange(PATIENT_ID));

        // then
        verify(serviceMatchingAttemptCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 매칭 시도면 403을 던진다")
    void 존재하지_않으면_403을_던진다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findById(MATCHING_ATTEMPT_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serviceMatchingAttemptCommandService.retry(
                command(REQUESTED_DATE, PATIENT_ID),
                carePlanRange(PATIENT_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    @DisplayName("본인 소유가 아닌 서비스 희망 일정이면 403을 던진다")
    void 본인_소유가_아니면_403을_던진다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findById(MATCHING_ATTEMPT_ID))
                .thenReturn(Optional.of(attempt(MatchingAttemptStatus.FAILED)));

        UUID otherRequesterId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> serviceMatchingAttemptCommandService.retry(
                command(REQUESTED_DATE, otherRequesterId),
                carePlanRange(PATIENT_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);

        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    @DisplayName("매칭 시도 상태가 FAILED가 아니면 409를 던진다")
    void FAILED가_아니면_409를_던진다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findById(MATCHING_ATTEMPT_ID))
                .thenReturn(Optional.of(attempt(MatchingAttemptStatus.MATCHED)));

        // when & then
        assertThatThrownBy(() -> serviceMatchingAttemptCommandService.retry(
                command(REQUESTED_DATE, PATIENT_ID),
                carePlanRange(PATIENT_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_NOT_RETRYABLE);

        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    @DisplayName("같은 매칭 시도로 이미 재시도가 접수됐으면 409를 던진다")
    void 이미_재시도_중이면_409를_던진다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findById(MATCHING_ATTEMPT_ID))
                .thenReturn(Optional.of(attempt(MatchingAttemptStatus.FAILED)));
        when(scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(
                ProviderReMatchEventPort.EVENT_TYPE, MATCHING_ATTEMPT_ID)
        ).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> serviceMatchingAttemptCommandService.retry(
                command(REQUESTED_DATE, PATIENT_ID),
                carePlanRange(PATIENT_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_ALREADY_REQUESTED);

        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    @DisplayName("재매칭 희망 날짜가 Care Plan 범위를 벗어나면 400을 던진다")
    void 범위를_벗어나면_400을_던진다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findById(MATCHING_ATTEMPT_ID))
                .thenReturn(Optional.of(attempt(MatchingAttemptStatus.FAILED)));
        when(scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(anyString(), any()))
                .thenReturn(false);

        LocalDate afterFinishDate = FINISH_DATE.plusDays(1);

        // when & then
        assertThatThrownBy(() -> serviceMatchingAttemptCommandService.retry(
                command(afterFinishDate, PATIENT_ID),
                carePlanRange(PATIENT_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_EXCEEDS_CARE_PLAN_RANGE);

        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }
}
