package com.todak_todag.social_worker_service.matching.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SocialWorkerMatchingResultTest {

    @Test
    @DisplayName("사회복지사 매칭 결과 엔티티 생성 시 전달된 값을 저장한다")
    void createMatchingResult() {

        UUID matchingResultId =
                UUID.randomUUID();

        UUID patientId =
                UUID.randomUUID();

        Instant requestedAt =
                Instant.now();

        SocialWorkerMatchingResult result =
                new SocialWorkerMatchingResult(
                        matchingResultId,
                        patientId,
                        null,
                        MatchingStatus.REQUESTED,
                        requestedAt,
                        null
                );

        assertEquals(
                matchingResultId,
                result.getMatchingResultId()
        );

        assertEquals(
                patientId,
                result.getPatientId()
        );

        assertEquals(
                MatchingStatus.REQUESTED,
                result.getStatus()
        );

        assertEquals(
                requestedAt,
                result.getRequestedAt()
        );

        assertNull(
                result.getSocialWorkerId()
        );

        assertNull(
                result.getAssignedAt()
        );
    }
}