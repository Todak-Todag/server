package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceMatchingAttemptCommandRepositoryImpl implements ServiceMatchingAttemptCommandRepository {

    private final SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    // 즉시 플러시 — V2의 부분 유니크 인덱스 위반을 커밋 시점이 아니라 호출 지점에서 잡기 위함
    // 아웃박스 적재(ScheduleOutboxEventCommandRepositoryImpl)와 같은 이유·같은 방식
    @Override
    public ServiceMatchingAttempt save(ServiceMatchingAttempt serviceMatchingAttempt) {
        return springDataServiceMatchingAttemptRepository.saveAndFlush(serviceMatchingAttempt);
    }

    @Override
    public Optional<ServiceMatchingAttempt> findById(UUID matchingAttemptId) {
        return springDataServiceMatchingAttemptRepository.findByIdAndDeletedAtIsNull(matchingAttemptId);
    }

    @Override
    public boolean existsMatched(
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            Instant matchedAt
    ) {
        return springDataServiceMatchingAttemptRepository
                .existsByServicePreferenceIdAndServiceOfferingIdAndDateAndMatchedAtAndStatusAndDeletedAtIsNull(
                        servicePreferenceId,
                        serviceOfferingId,
                        date,
                        matchedAt,
                        MatchingAttemptStatus.MATCHED
                );
    }

    @Override
    public boolean existsFailed(
            UUID servicePreferenceId,
            LocalDate date,
            Instant failedAt
    ) {
        return springDataServiceMatchingAttemptRepository
                .existsByServicePreferenceIdAndDateAndFailedAtAndStatusAndDeletedAtIsNull(
                        servicePreferenceId,
                        date,
                        failedAt,
                        MatchingAttemptStatus.FAILED
                );
    }

    @Override
    public Optional<ServiceMatchingAttempt> findLatestMatched(UUID servicePreferenceId) {
        return springDataServiceMatchingAttemptRepository
                .findFirstByServicePreferenceIdAndStatusAndDeletedAtIsNullOrderByMatchedAtDescCreatedAtDesc(
                        servicePreferenceId,
                        MatchingAttemptStatus.MATCHED
                );
    }

    @Override
    public long countUnresolvedFailed(UUID carePlanId) {
        return springDataServiceMatchingAttemptRepository
                .countUnresolvedByStatus(carePlanId, MatchingAttemptStatus.FAILED);
    }

    @Override
    public List<ServiceMatchingAttempt> findUnresolvedFailed(UUID carePlanId) {
        return springDataServiceMatchingAttemptRepository
                .findUnresolvedByStatus(carePlanId, MatchingAttemptStatus.FAILED);
    }

    @Override
    public List<UUID> findSweepTargetCarePlanIds(LocalDate lastActivityThreshold, int limit) {
        return springDataServiceMatchingAttemptRepository.findSweepTargetCarePlanIds(
                MatchingAttemptStatus.FAILED,
                lastActivityThreshold,
                PageRequest.of(0, limit)
        );
    }
}
