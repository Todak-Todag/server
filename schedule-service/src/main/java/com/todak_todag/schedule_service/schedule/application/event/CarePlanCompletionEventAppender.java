package com.todak_todag.schedule_service.schedule.application.event;

import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanServiceResultCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// CarePlanCompleted 이벤트의 "발행할지 말지"를 판단하고, 발행하기로 했다면 아웃박스에 적재
@Slf4j
@Component
@RequiredArgsConstructor
public class  CarePlanCompletionEventAppender {

    // 아직 결말나지 않은(진행 중) 일정 상태
    // SCHEDULED(예정) / RESCHEDULING(변경 중, 재매칭 진행 중인 중간 상태)만 진행 중
    private static final List<ScheduleStatus> UNFINISHED_STATUSES =
            List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING);

    private final ServiceScheduleCommandRepository serviceScheduleCommandRepository;
    private final CarePlanServiceResultCommandRepository carePlanServiceResultCommandRepository;
    private final ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;
    private final CarePlanCompletedEventPayloadSerializer carePlanCompletedEventPayloadSerializer;
    private final ScheduleOutboxCommandService scheduleOutboxCommandService;

    // 방금 결말이 난 일정(handledSchedule)을 기준으로 케어플랜이 완료되었는지 판단하고, 완료면 아웃박스에 적재
    //
    // 발행 조건은 두 가지를 모두 만족해야함
    //   (1) 이 케어플랜에 진행 중(SCHEDULED / RESCHEDULING) 일정이 하나도 남아있지 않음
    //       → 케어플랜이 끝났다는 것의 정의 그 자체. 어떤 일정이 트리거였는지와 무관하다
    //   (2) 이 케어플랜에 대해 CarePlanCompleted가 아직 적재된 적이 없음
    //       → 이미 결말난 일정들의 수행 결과가 뒤늦게 등록되어도 중복 발행되지 않게 막는 멱등 장치
    public void appendIfCarePlanCompleted(ServiceSchedule handledSchedule) {
        UUID carePlanId = handledSchedule.getCarePlanId();

        if (hasUnfinishedSchedule(carePlanId)) {
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

        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                resolveServiceResultId(lastSchedule),
                lastSchedule.getStatus()
        );

        // aggregateId는 이 이벤트가 대변하는 대상인 케어플랜
        scheduleOutboxCommandService.enqueue(
                CarePlanCompletedEventPort.EVENT_TYPE,
                carePlanId,
                carePlanCompletedEventPayloadSerializer.serialize(event)
        );

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

    // 아직 결말나지 않은 일정이 남아있는지
    private boolean hasUnfinishedSchedule(UUID carePlanId) {
        return serviceScheduleCommandRepository.countByCarePlanIdAndStatusIn(carePlanId, UNFINISHED_STATUSES) > 0;
    }
}
