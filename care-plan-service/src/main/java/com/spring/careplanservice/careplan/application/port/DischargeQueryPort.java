package com.spring.careplanservice.careplan.application.port;

import com.spring.careplanservice.careplan.application.result.DischargeFindResult;

import java.util.UUID;

public interface DischargeQueryPort {
    DischargeFindResult findById(UUID dischargeId);
}
