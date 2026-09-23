package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchFailedEvent;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchedEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceMatchingCommandService {

    // MVP 단계에서 서비스별 소요시간은 1시간으로 고정
    static final Duration DEFAULT_SERVICE_DURATION = Duration.ofHours(1);

    private final ServiceScheduleCommandRepository serviceScheduleCommandRepository;
    private final ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;

    // 매칭 성공 이벤트를 반영
    // 매칭 이력 기록과 일정 반영을 한 트랜잭션으로 묶음
    @Transactional
    public void applyMatched(ProviderMatchedEvent event) {

        // 멱등 처리 - 이미 처리된 이벤트는 추가 작업을 진행하지 않음
        if (alreadyApplied(event)) {
            log.info(
                    "[Schedule] 이미 처리된 ProviderMatched 이벤트를 다시 수신해 건너뜁니다 servicePreferenceId={} serviceOfferingId={} matchedAt={}",
                    event.servicePreferenceId(), event.serviceOfferingId(), event.matchedAt()
            );
            return;
        }

        // 이번 매칭 시도 결과 이력 저장
        recordAttempt(event);

        // 재매칭이면 대체될 기존 일정을 먼저 CHANGED 상태로 변경
        closeRescheduledIfPresent(event);

        // 매칭 결과를 새 일정으로 생성 (신규/재매칭 공통)
        createSchedule(event);
    }

    // 매칭 실패 이벤트를 반영
    @Transactional
    public void applyMatchFailed(ProviderMatchFailedEvent event) {

        // 멱등 처리 - 이미 처리된 이벤트는 추가 작업을 진행하지 않음
        if (alreadyApplied(event)) {
            log.info(
                    "[Schedule] 이미 처리된 ProviderMatchFailed 이벤트를 다시 수신해 건너뜁니다 servicePreferenceId={} date={} failedAt={}",
                    event.servicePreferenceId(), event.date(), event.failedAt()
            );
            return;
        }

        // 이번 매칭 시도 실패 이력 저장
        recordFailedAttempt(event);

        // 재매칭 실패면 변경 요청 이전 상태로 되돌림
        restoreRescheduledIfPresent(event);
    }

    // 동일 이벤트 중복 수신 방어 (1차) — 이미 처리된 재전송을 조용히 skip 시키는 정상 경로
    // 페이로드에 이벤트 ID가 없어, 같은 매칭 결과를 특정하는 값들의 조합을 대체 키로 사용
    // matchedAt(매칭 확정 일시)이 포함되어 있어, 같은 희망 일정이 나중에 다시 매칭되는 정상 케이스와는 구분
    //
    // 이 조회만으로는 check-then-act 사이에 끼어든 동시 수신을 막지 못해, 같은 조합의 부분 유니크
    // 인덱스를 V2에 추가해 DB를 최종 방어선으로 사용 (recordAttempt의 catch 참고)
    private boolean alreadyApplied(ProviderMatchedEvent event) {
        return serviceMatchingAttemptCommandRepository.existsMatched(
                event.servicePreferenceId(),
                event.serviceOfferingId(),
                event.date(),
                event.matchedAt()
        );
    }

    // p_service_matching_attempts에 매칭 시도 결과 기록
    // status는 MATCHED 고정이고, 실패 관련 컬럼(failureReason/failedAt)은 성공 이벤트이므로 null
    // preferredTimeSlot은 이 페이로드에 없어 null로 작성
    private void recordAttempt(ProviderMatchedEvent event) {
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                event.carePlanId(),
                event.regionId(),
                event.provideServiceId(),
                event.servicePreferenceId(),
                event.serviceOfferingId(),
                event.date(),
                null,
                MatchingAttemptStatus.MATCHED,
                null,
                event.matchedAt(),
                null
        );

        // 위 alreadyApplied와 이 적재 사이에 같은 이벤트가 끼어들면 V2의 유니크 인덱스에 걸림
        // Postgres는 제약 위반 시 트랜잭션 전체를 abort하므로 여기서 정상 흐름으로 되돌릴 수는 없고,
        // 커밋 시점의 불투명한 실패 대신 원인이 드러나는 로그를 남기는 것이 이 catch의 목적
        // (중복이라 아무것도 이중 기록되지 않은 상태이므로, DLQ로 간 메시지는 폐기해도 안전)
        try {
            serviceMatchingAttemptCommandRepository.save(attempt);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "[Schedule] 같은 ProviderMatched가 이미 기록되어 있어 중복 적재에 실패했습니다 "
                            + "servicePreferenceId={} serviceOfferingId={} date={} matchedAt={}",
                    event.servicePreferenceId(), event.serviceOfferingId(), event.date(), event.matchedAt(), e
            );
            throw e;
        }

        log.info(
                "[Schedule] 매칭 시도 결과 기록 matchingAttemptId={} servicePreferenceId={} serviceOfferingId={}",
                attempt.getId(), event.servicePreferenceId(), event.serviceOfferingId()
        );
    }

    // 동일 실패 이벤트 중복 수신 방어 (1차) — 성공 쪽과 같은 이유로 DB 부분 유니크 인덱스가 뒤를 받침
    // 실패 페이로드에는 serviceOfferingId가 없어 failedAt(실패 판정 일시)을 대체 키에 포함
    private boolean alreadyApplied(ProviderMatchFailedEvent event) {
        return serviceMatchingAttemptCommandRepository.existsFailed(
                event.servicePreferenceId(),
                event.date(),
                event.failedAt()
        );
    }

    // p_service_matching_attempts에 매칭 실패 결과 기록
    // status는 FAILED 고정, 매칭된 대상이 없으므로 serviceOfferingId는 null
    // preferredTimeSlot은 이 페이로드에 있으므로 그대로 기록
    private void recordFailedAttempt(ProviderMatchFailedEvent event) {
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                event.carePlanId(),
                event.regionId(),
                event.provideServiceId(),
                event.servicePreferenceId(),
                null,
                event.date(),
                event.preferredTimeSlot(),
                MatchingAttemptStatus.FAILED,
                event.failureReason(),
                null,
                event.failedAt()
        );

        // 성공 경로와 같은 이유 — 동시 수신이 유니크 인덱스에 걸렸을 때 원인을 남기고 다시 던짐
        try {
            serviceMatchingAttemptCommandRepository.save(attempt);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "[Schedule] 같은 ProviderMatchFailed가 이미 기록되어 있어 중복 적재에 실패했습니다 "
                            + "servicePreferenceId={} date={} failedAt={}",
                    event.servicePreferenceId(), event.date(), event.failedAt(), e
            );
            throw e;
        }

        log.info(
                "[Schedule] 매칭 실패 결과 기록 matchingAttemptId={} servicePreferenceId={} date={} failureReason={}",
                attempt.getId(), event.servicePreferenceId(), event.date(), event.failureReason()
        );
    }

    // 재매칭 실패라면 기존 RESCHEDULING 일정을 SCHEDULED로 복구
    //
    // 초기 매칭 실패(RESCHEDULING 0건)는 여기서 아무 일정도 만들지 않음
    // 따라서, CarePlanCompletionEventAppender가 미해소 FAILED 이력을 함께 보도록 보강
    private void restoreRescheduledIfPresent(ProviderMatchFailedEvent event) {
        List<ServiceSchedule> rescheduling =
                serviceScheduleCommandRepository.findRescheduling(event.servicePreferenceId());

        if (rescheduling.isEmpty()) {
            return;
        }

        // 한 희망 일정에 RESCHEDULING이 둘 이상 존재할 경우 오류 반환
        // 일정 변경 접수가 로우 쓰기 락으로 직렬화된 뒤로는 정상 경로에서 도달하지 않지만,
        // 과거 데이터나 수동 보정으로 생긴 이상 상태를 조용히 넘기지 않기 위한 방어선으로 남김
        if (rescheduling.size() > 1) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_MULTIPLE_RESCHEDULING);
        }

        ServiceSchedule restored = rescheduling.getFirst();
        restored.restoreToScheduled();

        log.info(
                "[Schedule] 재매칭 실패로 기존 일정을 예정 상태로 복구 serviceScheduleId={} servicePreferenceId={} date={}",
                restored.getId(), event.servicePreferenceId(), restored.getDate()
        );
    }

    // 재매칭이라면 기존 RESCHEDULING 일정을 CHANGED로 변경
    //
    // 신규/재매칭 구분 기준: 같은 servicePreferenceId로 RESCHEDULING 상태인 일정이 있는지
    //   0건 → 신규 매칭
    //   1건 → 재매칭
    private void closeRescheduledIfPresent(ProviderMatchedEvent event) {
        List<ServiceSchedule> rescheduling =
                serviceScheduleCommandRepository.findRescheduling(event.servicePreferenceId());

        if (rescheduling.isEmpty()) {
            return;
        }

        // 한 희망 일정에 RESCHEDULING이 둘 이상 존재할 경우 오류 반환
        // 일정 변경 접수가 로우 쓰기 락으로 직렬화된 뒤로는 정상 경로에서 도달하지 않지만,
        // 과거 데이터나 수동 보정으로 생긴 이상 상태를 조용히 넘기지 않기 위한 방어선으로 남김
        if (rescheduling.size() > 1) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_MULTIPLE_RESCHEDULING);
        }

        ServiceSchedule replaced = rescheduling.getFirst();
        replaced.markChanged();

        log.info(
                "[Schedule] 재매칭 성사로 기존 일정을 변경 완료 처리 serviceScheduleId={} servicePreferenceId={}",
                replaced.getId(), event.servicePreferenceId()
        );
    }

    // 매칭 결과를 새 서비스 일정으로 생성
    private void createSchedule(ProviderMatchedEvent event) {
        LocalDateTime finishedAt = event.startedAt().plus(DEFAULT_SERVICE_DURATION);

        ServiceSchedule schedule = ServiceSchedule.confirm(
                event.carePlanId(),
                event.servicePreferenceId(),
                event.serviceOfferingId(),
                event.date(),
                event.startedAt(),
                finishedAt
        );

        serviceScheduleCommandRepository.save(schedule);

        log.info(
                "[Schedule] 매칭 결과로 서비스 일정 생성 serviceScheduleId={} carePlanId={} date={} startedAt={} finishedAt={}",
                schedule.getId(), event.carePlanId(), event.date(), event.startedAt(), finishedAt
        );
    }
}
