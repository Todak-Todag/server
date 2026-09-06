package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchedEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 동일 이벤트 중복 수신 방어
    // 페이로드에 이벤트 ID가 없어, 같은 매칭 결과를 특정하는 값들의 조합을 대체 키로 사용
    // matchedAt(매칭 확정 일시)이 포함되어 있어, 같은 희망 일정이 나중에 다시 매칭되는 정상 케이스와는 구분
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

        serviceMatchingAttemptCommandRepository.save(attempt);

        log.info(
                "[Schedule] 매칭 시도 결과 기록 matchingAttemptId={} servicePreferenceId={} serviceOfferingId={}",
                attempt.getId(), event.servicePreferenceId(), event.serviceOfferingId()
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
