package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaServicePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServicePreferenceQueryRepositoryImpl implements ServicePreferenceQueryRepository {
    private final JpaServicePreferenceRepository jpaCarePlanPreferenceRepository;

    @Override
    public Optional<CarePlanServicePreference> findById(UUID id) {
        return jpaCarePlanPreferenceRepository.findByIdAndDeletedAtIsNull(id);
    }
}
