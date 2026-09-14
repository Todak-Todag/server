package com.spring.careplanservice.careplan.application.service.command;


import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanDeleteCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.application.event.*;
import com.spring.careplanservice.careplan.application.port.ScheduleResultQueryPort;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.CarePlanStatusUpdateResult;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.application.result.ScheduleResultFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.global.common.UserRole;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarePlanCommandService {
    private static final long CARE_PLAN_PERIOD_DAYS = 30L;

    private final CarePlanCommandRepository carePlanCommandRepository;
    private final CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    private final CarePlanServiceQueryRepository carePlanServiceQueryRepository;
    private final ServicePreferenceQueryRepository servicePreferenceQueryRepository;
    private final ServicePreferenceCommandRepository servicePreferenceCommandRepository;
    private final ScheduleResultQueryPort scheduleResultQueryPort;
    private final CarePlanCompletionEventAppender carePlanCompletionEventAppender;
    private final CarePlanConfirmedEventAppender carePlanConfirmedEventAppender;

    @Transactional
    public CarePlanCreateResult createCarePlan(
            CarePlanCreateCommand carePlanCreateCommand,
            DischargeFindResult dischargeFindResult
    ) {
        validateRequester(carePlanCreateCommand);
        validateDuplicateCarePlan(carePlanCreateCommand.dischargeId());
        validatePatient(
                carePlanCreateCommand.patientId(),
                dischargeFindResult.patientId()
        );
        validateActualDate(dischargeFindResult.actualDate());

        LocalDate startDate = dischargeFindResult.actualDate().plusDays(1);
        LocalDate finishDate = startDate.plusDays(CARE_PLAN_PERIOD_DAYS - 1);

        CarePlan carePlan = CarePlan.create(
                carePlanCreateCommand.patientId(),
                carePlanCreateCommand.dischargeId(),
                startDate,
                finishDate,
                carePlanCreateCommand.note()
        );

        CarePlan savedCarePlan = carePlanCommandRepository.save(carePlan);

        List<CarePlanService> carePlanServices = carePlanCreateCommand.provideServiceIds() == null ? List.of()
                : carePlanCreateCommand.provideServiceIds()
                .stream()
                .distinct()
                .map(provideServiceId ->
                        CarePlanService.create(
                                savedCarePlan.getId(),
                                provideServiceId
                        )
                )
                .toList();

        carePlanServiceCommandRepository.saveAll(
                carePlanServices
        );

        return CarePlanCreateResult.from(
                savedCarePlan
        );
    }

    @Transactional
    public CarePlanStatusUpdateResult updateCarePlanStatus(
            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand,
            UUID regionId
    ) {
        // 외부 호출과 실제 상태 변경 사이에 Care Plan 상태가 변경될 수 있으므로
        // 쓰기 트랜잭션 진입 후 Care Plan을 다시 조회한다.
        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanStatusUpdateCommand.carePlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_PLAN_NOT_FOUND));

        // 외부 호출 전에 조회했던 상태를 신뢰하지 않고,
        // 실제 변경 직전에 현재 상태 기준으로 상태 전이를 다시 검증한다.
        validateStatusTransition(
                carePlan,
                carePlanStatusUpdateCommand.status()
        );

        validateStatusUpdateRole(
                carePlanStatusUpdateCommand
        );

        carePlan.updateStatus(
                carePlanStatusUpdateCommand.status()
        );

        if (carePlan.getStatus() == CarePlanStatus.CONFIRMED) {
            // User Service에서 조회한 regionId를 사용해 Confirmed 이벤트를 구성한다.
            // 외부 호출 자체는 이미 트랜잭션 진입 전에 완료된 상태다.
            CarePlanConfirmedEvent carePlanConfirmedEvent = createCarePlanConfirmedEvent(
                    carePlan.getId(),
                    regionId
            );

            // 상태 변경과 Outbox 저장을 동일 트랜잭션으로 처리해
            // DB 상태 변경과 이벤트 발행 준비 데이터의 원자성을 유지한다.
            carePlanConfirmedEventAppender.append(
                    carePlanConfirmedEvent
            );
        }

        return CarePlanStatusUpdateResult.from(
                carePlan
        );
    }

    @Transactional
    public void deleteCarePlan(
            CarePlanDeleteCommand carePlanDeleteCommand
    ) {
        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanDeleteCommand.carePlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_PLAN_NOT_FOUND));

        validateDeletable(carePlan);

        List<CarePlanService> carePlanServices = carePlanServiceCommandRepository.findAllByCarePlanId(
                carePlan.getId()
        );

        List<UUID> planServiceIds = carePlanServices.stream()
                .map(CarePlanService::getId)
                .toList();

        List<CarePlanServicePreference> preferences = servicePreferenceCommandRepository.findAllByPlanServiceIds(
                planServiceIds
        );

        UUID deletedBy = carePlanDeleteCommand.userId();

        preferences.forEach(preference -> preference.delete(deletedBy));
        carePlanServices.forEach(carePlanService -> carePlanService.delete(deletedBy));
        carePlan.delete(deletedBy);
    }

    @Transactional
    public void completeCarePlan(
            CarePlanCompletedEvent carePlanCompletedEvent
    ) {
        // Schedule-Service 에서 수신한 완료 이벤트의 payload 유효성 검증
        validateCompletedEvent(carePlanCompletedEvent);

        // serviceResultId가 존재하는 경우 실제 Schedule 수행 결과인지 내부 API로 검증하고,
        // 그 수행 결과가 실제로 이 이벤트의 carePlanId에 속하는지 교차 검증한다
        if (carePlanCompletedEvent.serviceResultId() != null) {
            ScheduleResultFindResult scheduleResultFindResult = scheduleResultQueryPort.findById(
                    carePlanCompletedEvent.serviceResultId()
            );

            validateCarePlanId(
                    carePlanCompletedEvent,
                    scheduleResultFindResult
            );
        }

        // 완료 대상 Care Plan 조회
        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanCompletedEvent.carePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        // IN_PROGRESS 상태인 경우에만 COMPLETED로 전이
        // 이미 완료되었거나 완료 대상이 아닌 경우 중복 Outbox 이벤트 생성을 방지
        boolean completed = carePlan.complete();

        if (!completed) {
            return;
        }

        carePlanCompletionEventAppender.append(carePlan);
    }

    // 동일한 퇴원 건에 이미 Care Plan이 존재하는지 검사
    private void validateDuplicateCarePlan(
            UUID dischargeId
    ) {
        if (carePlanCommandRepository.existsByDischargeId(dischargeId)) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_ALREADY_EXISTS
            );
        }
    }

    // 요청한 환자와 퇴원 건의 환자가 동일한지 검사
    private void validatePatient(
            UUID requestPatientId,
            UUID dischargePatientId
    ) {
        if (!requestPatientId.equals(dischargePatientId)) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_PATIENT_MISMATCH
            );
        }
    }

    // 실제 퇴원이 완료된 건인지 검사
    private void validateActualDate(
            LocalDate actualDate
    ) {
        if (actualDate == null) {
            throw new BusinessException(
                    ErrorCode.DISCHARGE_NOT_COMPLETED
            );
        }
    }

    // 환자가 남의 Care Plan을 만들려고 하는지 검사
    private void validateRequester(
            CarePlanCreateCommand carePlanCreateCommand
    ) {
        if (carePlanCreateCommand.userRole() == UserRole.PATIENT
                && !carePlanCreateCommand.userId().equals(carePlanCreateCommand.patientId())) {

            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }
    }

    // 현재 Care Plan 상태에서 요청한 다음 상태로의 전이가 허용되는지 검증
    // 허용되지 않는 상태 전이라면 비즈니스 예외 발생
    private void validateStatusTransition(
            CarePlan carePlan,
            CarePlanStatus nextStatus
    ) {
        if (!carePlan.canTransitionTo(nextStatus)) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_INVALID_STATUS_TRANSITION
            );
        }
    }

    // 삭제 가능한 상태(UNDER_REVIEW)인지 검사
    private void validateDeletable(
            CarePlan carePlan
    ) {
        if (!carePlan.allowsDeletion()) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_DELETE_NOT_ALLOWED
            );
        }
    }
    private CarePlanConfirmedEvent createCarePlanConfirmedEvent(
            UUID carePlanId,
            UUID regionId
    ) {
        List<CarePlanService> carePlanServices = carePlanServiceQueryRepository.findAllByCarePlanId(
                carePlanId
        );

        List<UUID> planServiceIds = carePlanServices.stream()
                .map(CarePlanService::getId)
                .toList();

        List<CarePlanServicePreference> preferences = servicePreferenceQueryRepository.findAllByPlanServiceIds(
                planServiceIds
        );

        List<CarePlanConfirmedEvent.Service> services = createConfirmedServices(
                carePlanServices,
                preferences
        );

        return new CarePlanConfirmedEvent(
                carePlanId,
                regionId,
                services
        );
    }

    private List<CarePlanConfirmedEvent.Service> createConfirmedServices(
            List<CarePlanService> carePlanServices,
            List<CarePlanServicePreference> preferences
    ) {
        Map<UUID, List<CarePlanServicePreference>> preferencesByPlanServiceId = preferences.stream()
                .collect(Collectors.groupingBy(CarePlanServicePreference::getPlanServiceId));

        return carePlanServices.stream()
                .map(carePlanService -> {
                    List<CarePlanConfirmedEvent.Preference> eventPreferences = preferencesByPlanServiceId
                            .getOrDefault(carePlanService.getId(), List.of())
                            .stream()
                            .map(preference ->
                                    new CarePlanConfirmedEvent.Preference(
                                            preference.getId(),
                                            preference.getPreferredDate(),
                                            preference.getPreferredTimeSlot()
                                    ))
                            .toList();

                    return new CarePlanConfirmedEvent.Service(
                            carePlanService.getId(),
                            carePlanService.getProvideServiceId(),
                            eventPreferences
                    );
                })
                .toList();
    }

    // TODO : validate 분리 시급
    private void validateCompletedEvent(
            CarePlanCompletedEvent event
    ) {
        if (event.status() == ScheduleStatus.CANCELED) {
            return;
        }

        if (event.serviceResultId() == null) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_COMPLETED_EVENT_INVALID
            );
        }
    }

    // serviceResultId로 조회한 Schedule 수행 결과가 실제로 이 이벤트가 주장하는
    // carePlanId에 속하는지 검증한다. 서로 다른 Care Plan의 수행 결과가 실려온 경우
    // 완료 처리를 차단한다.
    private void validateCarePlanId(
            CarePlanCompletedEvent event,
            ScheduleResultFindResult scheduleResultFindResult
    ) {
        if (!event.carePlanId().equals(scheduleResultFindResult.carePlanId())) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_COMPLETED_EVENT_CARE_PLAN_MISMATCH
            );
        }
    }

    private void validateStatusUpdateRole(
            CarePlanStatusUpdateCommand command
    ) {
        UserRole userRole = command.userRole();
        CarePlanStatus nextStatus = command.status();

        if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
            return;
        }

        if (nextStatus == CarePlanStatus.CONFIRMED
                && userRole != UserRole.PATIENT) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        if (nextStatus == CarePlanStatus.IN_PROGRESS
                && userRole != UserRole.SERVICE_PROVIDER) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
    }
}
