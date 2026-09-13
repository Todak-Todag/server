package com.todak_todag.schedule_service.schedule.application.event;

import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanCompletionLockRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanServiceResultCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// CarePlanCompleted 이벤트의 "발행할지 말지"를 판단하고, 발행하기로 했다면 아웃박스에 적재
@Slf4j
@Component
@RequiredArgsConstructor
public class  CarePlanCompletionEventAppender {

    // 상태만으로 "아직 진행 중"인 일정
    // SCHEDULED(예정) / RESCHEDULING(변경 중, 재매칭 진행 중인 중간 상태)
    private static final List<ScheduleStatus> UNFINISHED_STATUSES =
            List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING);

    // 상태는 결말났지만 수행 결과가 등록되어야 비로소 끝나는 일정
    private static final List<ScheduleStatus> RESULT_REQUIRED_STATUSES =
            List.of(ScheduleStatus.COMPLETED, ScheduleStatus.NO_SHOW);

    private final ServiceScheduleCommandRepository serviceScheduleCommandRepository;
    private final ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;
    private final CarePlanServiceResultCommandRepository carePlanServiceResultCommandRepository;
    private final ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;
    private final CarePlanCompletionLockRepository carePlanCompletionLockRepository;
    private final CarePlanCompletedEventPayloadSerializer carePlanCompletedEventPayloadSerializer;
    private final ScheduleOutboxCommandService scheduleOutboxCommandService;

    // 방금 결말이 난 일정(handledSchedule)을 기준으로 케어플랜이 완료되었는지 판단하고, 완료면 아웃박스에 적재
    //
    // 발행 조건은 두 가지를 모두 만족해야함
    //   (1) 이 케어플랜에 아직 해소되지 않은 것이 하나도 없음
    //       → 케어플랜이 끝났다는 것의 정의 그 자체. 어떤 일정이 트리거였는지와 무관
    //       → 일정 쪽: 상태가 SCHEDULED/RESCHEDULING이거나, COMPLETED/NO_SHOW인데 수행 결과가 아직 없는 경우
    //       → 매칭 쪽: 초기 매칭에 실패해 일정 레코드조차 만들어지지 않은 서비스가 남아있는 경우
    //   (2) 이 케어플랜에 대해 CarePlanCompleted가 아직 적재된 적이 없음
    //       → 이미 결말난 일정들의 수행 결과가 뒤늦게 등록되어도 중복 발행되지 않게 막는 멱등 장치
    //
    // 호출자의 트랜잭션에 합류해 판정~적재가 커맨드와 같은 원자 단위로 묶임
    @Transactional
    public void appendIfCarePlanCompleted(ServiceSchedule handledSchedule) {
        UUID carePlanId = handledSchedule.getCarePlanId();

        // 판정(조회)과 적재(INSERT) 사이에 다른 트랜잭션이 끼어들지 못하게 케어플랜 단위로 직렬화
        // 락이 없으면 같은 케어플랜의 마지막 두 일정이 거의 동시에 끝났을 때 양쪽 모두 서로를 "아직 미완료"로
        // 읽고 조기 반환해, CarePlanCompleted가 영영 적재되지 않을 수 있음 (READ COMMITTED)
        carePlanCompletionLockRepository.lockForCompletionCheck(carePlanId);

        if (hasUnfinishedWork(carePlanId)) {
            return;
        }

        if (alreadyAppended(carePlanId)) {
            return;
        }

        // 페이로드 기준이 되는 일정은 트리거(handledSchedule)가 아니라 케어플랜의 마지막 일정
        // 트리거가 마지막 일정이 아닐 수 있으므로 여기서 다시 조회
        // 마지막 일정 선정 규칙 — finished_at DESC, created_at DESC / CHANGED 제외
        ServiceSchedule lastSchedule = serviceScheduleCommandRepository.findLastSchedule(carePlanId)
                .orElse(handledSchedule);

        // carePlanId도 페이로드 기준 일정(lastSchedule)에서 읽어와 serviceResultId/status와 출처를 일치
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                lastSchedule.getCarePlanId(),
                resolveServiceResultId(lastSchedule),
                lastSchedule.getStatus()
        );

        // aggregateId는 이 이벤트가 대변하는 대상인 케어플랜
        try {
            scheduleOutboxCommandService.enqueue(
                    CarePlanCompletedEventPort.EVENT_TYPE,
                    carePlanId,
                    carePlanCompletedEventPayloadSerializer.serialize(event)
            );
        } catch (DataIntegrityViolationException e) {
            // 아웃박스의 CarePlanCompleted 부분 유니크 인덱스에 걸린 경우 = 이미 적재되어 있는 경우
            // 위 락이 같은 케어플랜의 판정을 직렬화하므로 정상 경로에서는 도달하지 않으며, 락 밖에서
            // 들어온 중복을 DB가 마지막으로 막았을 때만 걸림
            // Postgres는 제약 위반 시 트랜잭션 전체를 abort하므로 여기서 정상 흐름으로 되돌릴 수는 없음
            // — 커밋 시점의 불투명한 실패 대신 원인이 드러나는 로그를 남기는 것이 이 catch의 목적
            log.error("[Schedule] CarePlanCompleted가 이미 적재되어 있어 중복 적재에 실패했습니다 carePlanId={}", carePlanId, e);
            throw e;
        }

        log.info(
                "[Schedule] CarePlanCompleted 이벤트 아웃박스 적재 carePlanId={} serviceResultId={} status={} lastServiceScheduleId={}",
                carePlanId, event.serviceResultId(), event.status(), lastSchedule.getId()
        );
    }

    // 페이로드에 실을 serviceResultId 결정
    //
    // status와 짝이 맞아야 하므로 케어플랜 전체가 아니라 이 일정 하나만 확인
    //   COMPLETED / NO_SHOW → 일정당 결과 1건만 허용하므로 정확히 1건이 조회
    //   CANCELED            → 수행된 적이 없어 결과가 존재하지 않으므로 null
    private UUID resolveServiceResultId(ServiceSchedule lastSchedule) {
        UUID serviceResultId = carePlanServiceResultCommandRepository
                .findByServiceScheduleId(lastSchedule.getId())
                .map(CarePlanServiceResult::getServiceResultId)
                .orElse(null);

        // CANCELED가 아닌데 결과가 없다면 처리 순서가 어긋난 비정상 상태
        // status만으로도 수신 측이 판단할 수 있으므로 발행은 막지 않고 운영자가 확인할 수 있게 로그 작성
        if (serviceResultId == null && lastSchedule.getStatus() != ScheduleStatus.CANCELED) {
            log.warn(
                    "[Schedule] 마지막 일정에 수행 결과가 없어 serviceResultId 없이 발행합니다 serviceScheduleId={} status={}",
                    lastSchedule.getId(), lastSchedule.getStatus()
            );
        }

        return serviceResultId;
    }

    // 이 케어플랜에 대해 CarePlanCompleted가 이미 적재된 적이 있는지 (중복 발행 방지)
    // 적재 시 aggregateId를 carePlanId로 넣고 있으므로 그대로 조회 키로 사용
    private boolean alreadyAppended(UUID carePlanId) {
        return scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(
                CarePlanCompletedEventPort.EVENT_TYPE, carePlanId
        );
    }

    // 케어플랜에 아직 해소되지 않은 것이 남아있는지
    // 일정 기준과 매칭 기준 중 하나라도 걸리면 미완료이므로 OR로 결합
    private boolean hasUnfinishedWork(UUID carePlanId) {
        return hasUnfinishedSchedule(carePlanId) || hasUnresolvedMatchFailure(carePlanId);
    }

    // 아직 끝나지 않은 일정이 남아있는지
    private boolean hasUnfinishedSchedule(UUID carePlanId) {
        if (serviceScheduleCommandRepository.countByCarePlanIdAndStatusIn(carePlanId, UNFINISHED_STATUSES) > 0) {
            return true;
        }

        return serviceScheduleCommandRepository.countMissingResult(carePlanId, RESULT_REQUIRED_STATUSES) > 0;
    }

    // 아직 해소되지 않은 초기 매칭 실패가 남아있는지
    //
    // 초기 매칭 실패는 p_service_schedules에 레코드를 남기지 않으므로(14번) 일정 기준만으로 판정하면
    // 그 서비스가 판정 대상에서 통째로 빠져 나머지 일정이 다 끝나는 순간 조기 발행
    // aggregate_id 기준 멱등이 영구적이라 이후 16번으로 재매칭에 성공해도 다시는 발행되지 않음
    // "미해소" 판정은 15번 FAILED 필터와 같은 조건(일정 레코드가 아예 없는 FAILED)을 사용
    // 매칭에 성공하면 반드시 일정이 생기므로, 이미 해소된 과거 실패 이력은 이 조건으로 자연히 걸러짐
    private boolean hasUnresolvedMatchFailure(UUID carePlanId) {
        return serviceMatchingAttemptCommandRepository.countUnresolvedFailed(carePlanId) > 0;
    }
}
