package com.spring.careplanservice.careplan.domain.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

public interface CarePlanServicePreferenceRepository {
    CarePlanServicePreference save(
            CarePlanServicePreference carePlanServicePreference
    );
}
