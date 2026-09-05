package com.todak_todag.discharge_service.discharge.application.service.query;

import com.todak_todag.discharge_service.discharge.application.result.DischargeFindResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeInternalFindResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.query.DischargeQueryRepository;
import com.todak_todag.discharge_service.global.common.UserRole;
import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DischargeQueryService {

    private final DischargeQueryRepository dischargeQueryRepository;

    public DischargeFindResult findDischarge(
            UUID dischargeId,
            UUID userId,
            UserRole userRole
    ) {
        Discharge discharge =
                dischargeQueryRepository.findById(dischargeId)
                        .orElseThrow(
                                () -> new BusinessException(
                                        ErrorCode.DISCHARGE_NOT_FOUND,
                                        Map.of(
                                                "reason",
                                                "퇴원건을 찾을 수 없습니다."
                                        )
                                )
                        );

        validateReadPermission(
                discharge,
                userId,
                userRole
        );

        return DischargeFindResult.from(discharge);
    }

    public DischargeInternalFindResult findById(UUID dischargeId) {
        Discharge discharge =
                dischargeQueryRepository.findById(dischargeId)
                        .orElseThrow(
                                () -> new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND,
                                        Map.of(
                                                "reason",
                                                "요청한 dischargeId에 해당하는 퇴원건이 존재하지 않습니다."
                                        )
                                )
                        );

        return DischargeInternalFindResult.from(discharge);
    }

    private void validateReadPermission(
            Discharge discharge,
            UUID userId,
            UserRole userRole
    ) {
        boolean hasPermission =
                switch (userRole) {
                    case PATIENT ->
                            discharge.getPatientId().equals(userId);

                    case HOSPITAL_STAFF ->
                            discharge.getHospitalStaffId().equals(userId);

                    default -> false;
                };

        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN,
                    Map.of(
                            "reason",
                            "퇴원건 조회 권한이 없습니다."
                    )
            );
        }
    }
}