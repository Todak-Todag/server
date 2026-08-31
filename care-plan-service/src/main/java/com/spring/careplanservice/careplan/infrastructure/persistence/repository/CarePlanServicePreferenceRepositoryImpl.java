package com.spring.careplanservice.careplan.infrastructure.persistence.repository;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.CarePlanServicePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CarePlanServicePreferenceRepositoryImpl implements CarePlanServicePreferenceRepository {
    @Override
    public CarePlanServicePreference save(
            CarePlanServicePreference carePlanServicePreference
    ) {
        return null;
    }
}
