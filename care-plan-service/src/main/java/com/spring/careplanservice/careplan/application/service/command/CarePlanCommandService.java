package com.spring.careplanservice.careplan.application.service.command;


import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import com.spring.careplanservice.careplan.application.port.CarePlanEventPort;
import com.spring.careplanservice.careplan.application.port.UserQueryPort;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.CarePlanStatusUpdateResult;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.application.result.UserFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarePlanCommandService {
    private static final long CARE_PLAN_PERIOD_DAYS = 30L;

    private final CarePlanCommandRepository carePlanCommandRepository;
    private final CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    private final CarePlanServiceQueryRepository carePlanServiceQueryRepository;
    private final ServicePreferenceQueryRepository servicePreferenceQueryRepository;
    private final CarePlanEventPort carePlanEventPort;
    private final UserQueryPort userQueryPort;

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
            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand
    ) {
        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanStatusUpdateCommand.carePlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_PLAN_NOT_FOUND));

        validateStatusTransition(
                carePlan,
                carePlanStatusUpdateCommand.status()
        );

        carePlan.updateStatus(
                carePlanStatusUpdateCommand.status()
        );

        // TODO: User-Service의 구현/머지 후 실제 연동 확인
        if (carePlan.getStatus() == CarePlanStatus.CONFIRMED) {
            UserFindResult userFindResult = userQueryPort.findById(
                    carePlan.getPatientId()
            );

            CarePlanConfirmedEvent carePlanConfirmedEvent = createCarePlanConfirmedEvent(
                    carePlan.getId(),
                    userFindResult.regionId()
            );

            carePlanEventPort.publishCarePlanConfirmed(
                    carePlanConfirmedEvent
            );
        }

        return CarePlanStatusUpdateResult.from(
                carePlan
        );
    }

    @Transactional
    public void completeCarePlan(UUID carePlanId) {
        CarePlan carePlan = carePlanCommandRepository
                .findById(carePlanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_PLAN_NOT_FOUND));

        carePlan.complete();
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

    private CarePlanConfirmedEvent createCarePlanConfirmedEvent(
            UUID carePlanId,
            UUID regionId
    ) {
        List<CarePlanConfirmedEvent.Service> services = carePlanServiceQueryRepository
                .findAllByCarePlanId(carePlanId)
                .stream()
                .map(carePlanService -> {
                    List<CarePlanConfirmedEvent.Preference> preferences = servicePreferenceQueryRepository
                            .findAllByPlanServiceId(carePlanService.getId())
                            .stream()
                            .map(preference ->
                                    new CarePlanConfirmedEvent.Preference(
                                            preference.getId(),
                                            preference.getPreferredDate(),
                                            preference.getPreferredTimeSlot()
                                    )
                            )
                            .toList();

                    return new CarePlanConfirmedEvent.Service(
                            carePlanService.getId(),
                            carePlanService.getProvideServiceId(),
                            preferences
                    );
                })
                .toList();

        return new CarePlanConfirmedEvent(
                carePlanId,
                regionId,
                services
        );
    }
}
