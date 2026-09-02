package com.todak_todag.discharge_service.discharge.application.query_service;

import com.todak_todag.discharge_service.discharge.application.result.DischargeInternalFindResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.DischargeRepository;
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

    private final DischargeRepository dischargeRepository;

    public DischargeInternalFindResult findById(
            UUID dischargeId
    ) {
        Discharge discharge =
                dischargeRepository.findById(dischargeId)
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
}