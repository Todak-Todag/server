package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.MatchingAttemptRetryCommand;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEvent;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptRetryResult;
import com.todak_todag.schedule_service.schedule.application.support.MatchingAttemptValidator;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 매칭 시도에 대한 커맨드 — 순수한 트랜잭션 경계를 담당
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceMatchingAttemptCommandService {

    private final ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;
    private final ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;
    private final ScheduleOutboxCommandService scheduleOutboxCommandService;
    private final ProviderReMatchEventPayloadSerializer providerReMatchEventPayloadSerializer;
    private final ServiceScheduleValidator serviceScheduleValidator;
    private final MatchingAttemptValidator matchingAttemptValidator;

    // 재매칭 시도 접수
    // 트랜잭션 처리 범위: 검증 + ProviderReMatched 이벤트를 아웃박스에 적재
    // 새로운 매칭 시도 이력은 동기적으로 만들지 않음 — Provider-Service의 매칭 결과 이벤트를 수신할 때 추가됨
    @Transactional
    public MatchingAttemptRetryResult retry(MatchingAttemptRetryCommand retryCommand, CarePlanPort.CarePlanRange carePlanRange) {

        // facade가 이미 존재를 확인했지만, facade의 조회와 이 트랜잭션 사이 시점 차이를 방어하기 위해 다시 조회
        ServiceMatchingAttempt attempt = serviceMatchingAttemptCommandRepository.findById(retryCommand.matchingAttemptId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 소유권 검증(403)을 상태/날짜 검증보다 먼저 — 비소유자가 409/400 응답으로 대상의 존재나 상태를 알아내지 못하게 함
        serviceScheduleValidator.validateOwnership(retryCommand.requesterId(), carePlanRange.patientId());
        matchingAttemptValidator.validateRetryable(attempt.getStatus());
        matchingAttemptValidator.validateNotAlreadyRequested(alreadyRequested(attempt));
        matchingAttemptValidator.validateRetryDate(retryCommand.date(), carePlanRange);

        // 페이로드의 carePlanId/regionId/provideServiceId/servicePreferenceId는 기존(실패) 매칭 시도 레코드에서 그대로 읽어옴
        String payload = providerReMatchEventPayloadSerializer.serialize(
                ProviderReMatchEvent.forRetry(
                        attempt.getCarePlanId(),
                        attempt.getRegionId(),
                        attempt.getProvideServiceId(),
                        attempt.getServicePreferenceId(),
                        retryCommand.date(),
                        retryCommand.preferredTimeSlot()
                )
        );

        // aggregateId는 재시도 대상이 된 매칭 시도 — 중복 접수(409) 판별의 조회 키가 되므로 반드시 이 값이어야 함
        scheduleOutboxCommandService.enqueue(ProviderReMatchEventPort.EVENT_TYPE, attempt.getId(), payload);

        log.info(
                "[Schedule] 재매칭 시도 접수 matchingAttemptId={} servicePreferenceId={} requestedDate={} preferredTimeSlot={}",
                attempt.getId(), attempt.getServicePreferenceId(), retryCommand.date(), retryCommand.preferredTimeSlot()
        );

        return new MatchingAttemptRetryResult(
                attempt.getId(),
                attempt.getServicePreferenceId(),
                retryCommand.date(),
                retryCommand.preferredTimeSlot()
        );
    }

    // "이미 재시도 중"인지 판별 — 같은 매칭 시도로 ProviderRematched가 이미 적재됐는지 확인
    private boolean alreadyRequested(ServiceMatchingAttempt attempt) {
        return scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(
                ProviderReMatchEventPort.EVENT_TYPE, attempt.getId()
        );
    }
}
