package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServicePreferenceQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServicePreferenceQueryRepositoryImpl implements CarePlanServicePreferenceQueryRepository {

    @Override
    public Optional<CarePlanServicePreference> findById(UUID id) {
        return Optional.empty();
    }
}
