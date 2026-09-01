package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CarePlanServicePreferenceCommandRepositoryImpl implements ServicePreferenceCommandRepository {
    @Override
    public CarePlanServicePreference save(
            CarePlanServicePreference carePlanServicePreference
    ) {
        return null;
    }
}
