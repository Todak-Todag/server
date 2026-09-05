package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanServiceResultCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 순수한 트랜잭션 경계를 담당
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceResultCommandService {

    private final ServiceScheduleCommandRepository serviceScheduleCommandRepository;
    private final CarePlanServiceResultCommandRepository carePlanServiceResultCommandRepository;
    private final ServiceScheduleValidator serviceScheduleValidator;

    // 서비스 수행 결과 등록
    // 트랜잭션 처리 범위: 검증(제공자 본인 확인 + status 확인 + 중복 등록 확인) + p_care_plan_service_results 신규 생성
    @Transactional
    public ServiceResultRegisterResult register(ServiceResultRegisterCommand registerCommand, UUID assignedProviderId) {

        // facade가 이미 존재를 확인했지만, facade의 조회와 이 트랜잭션 사이 시점 차이를 방어하기 위해 다시 조회
        ServiceSchedule serviceSchedule = serviceScheduleCommandRepository.findById(registerCommand.serviceScheduleId())
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND));

        // 본인이 배정된 서비스 제공자인지 검증
        serviceScheduleValidator.validateAssignedProvider(registerCommand.requesterId(), assignedProviderId);

        // status가 COMPLETED/NO_SHOW가 아니면 409
        serviceSchedule.assertResultRegistrable();

        // 동일 serviceScheduleId에 대한 중복 등록 방지 — 하나의 일정에는 하나의 결과만 허용
        if (carePlanServiceResultCommandRepository.existsByServiceScheduleId(registerCommand.serviceScheduleId())) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_RESULTS_ALREADY_EXISTS);
        }

        CarePlanServiceResult carePlanServiceResult = CarePlanServiceResult.record(
                registerCommand.serviceScheduleId(),
                registerCommand.startedAt(),
                registerCommand.finishedAt(),
                registerCommand.note()
        );
        CarePlanServiceResult saved = carePlanServiceResultCommandRepository.save(carePlanServiceResult);

        log.info("[Schedule] 서비스 수행 결과 등록 완료 serviceScheduleId={} serviceResultId={}",
                registerCommand.serviceScheduleId(), saved.getServiceResultId());

        return ServiceResultRegisterResult.from(saved);
    }
}
