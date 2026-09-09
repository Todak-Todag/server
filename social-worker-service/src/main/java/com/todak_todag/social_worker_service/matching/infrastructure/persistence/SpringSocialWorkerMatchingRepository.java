package com.todak_todag.social_worker_service.matching.infrastructure.persistence;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerLoadProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SpringSocialWorkerMatchingRepository
        extends JpaRepository<SocialWorkerMatchingResult, UUID> {

    boolean existsByPatientIdAndStatusIn(
            UUID patientId,
            Collection<MatchingStatus> statuses
    );

    Optional<SocialWorkerMatchingResult>
    findFirstByPatientIdAndDeletedAtIsNullOrderByRequestedAtDesc(
            UUID patientId
    );

    @Query("""
            select
                m.socialWorkerId as socialWorkerId,
                count(m) as activeCount
            from SocialWorkerMatchingResult m
            where m.socialWorkerId in :socialWorkerIds
              and m.status = :status
              and m.deletedAt is null
            group by m.socialWorkerId
            """)
    List<SocialWorkerLoadProjection> countBySocialWorkerIdsAndStatus(
            @Param("socialWorkerIds") Set<UUID> socialWorkerIds,
            @Param("status") MatchingStatus status
    );
}