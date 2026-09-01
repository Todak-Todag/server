package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

public interface ServicePreferenceCommandRepository {
    CarePlanServicePreference save(
            CarePlanServicePreference carePlanServicePreference
    );
}
