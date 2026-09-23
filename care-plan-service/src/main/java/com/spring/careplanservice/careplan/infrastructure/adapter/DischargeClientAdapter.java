package com.spring.careplanservice.careplan.infrastructure.adapter;


import com.spring.careplanservice.careplan.application.port.DischargeQueryPort;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.infrastructure.client.DischargeFeignClient;
import com.spring.careplanservice.careplan.infrastructure.client.DischargeInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DischargeClientAdapter implements DischargeQueryPort {
    private final DischargeFeignClient dischargeFeignClient;

    @Override
    public DischargeFindResult findById(UUID dischargeId) {
        DischargeInternalResponse dischargeInternalResponse = dischargeFeignClient.findById(dischargeId);

        DischargeInternalResponse.Data data = dischargeInternalResponse.data();

        return new DischargeFindResult(
                data.dischargeId(),
                data.patientId(),
                data.actualDate()
        );
    }
}
