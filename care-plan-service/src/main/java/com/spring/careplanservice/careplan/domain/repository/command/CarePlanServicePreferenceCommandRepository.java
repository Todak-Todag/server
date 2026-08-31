package com.spring.careplanservice.careplan.domain.repository.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

public interface CarePlanServicePreferenceCommandRepository {
    CarePlanServicePreference save(
            CarePlanServicePreference carePlanServicePreference
    );
}
