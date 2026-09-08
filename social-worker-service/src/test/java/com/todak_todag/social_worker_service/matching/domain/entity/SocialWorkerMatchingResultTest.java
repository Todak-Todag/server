package com.todak_todag.social_worker_service.matching.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SocialWorkerMatchingResultTest {

    @Test
    @DisplayName("매칭 요청 생성 시 REQUESTED 상태와 요청 시각을 기록한다")
    void createRequestedMatching() {

        UUID patientId = UUID.randomUUID();

        Instant before =
                Instant.now();

        SocialWorkerMatchingResult result =
                SocialWorkerMatchingResult
                        .requested(patientId);

        Instant after =
                Instant.now();

        assertNotNull(
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

        assertNotNull(
                result.getRequestedAt()
        );

        assertFalse(
                result.getRequestedAt()
                        .isBefore(before)
        );

        assertFalse(
                result.getRequestedAt()
                        .isAfter(after)
        );

        assertNull(
                result.getSocialWorkerId()
        );

        assertNull(
                result.getAssignedAt()
        );
    }

    @Test
    @DisplayName("사회복지사 배정 시 ACTIVE 상태와 assignedAt을 기록한다")
    void assignSocialWorker() {

        UUID patientId = UUID.randomUUID();
        UUID socialWorkerId = UUID.randomUUID();

        SocialWorkerMatchingResult result =
                SocialWorkerMatchingResult
                        .requested(patientId);

        result.assign(
                socialWorkerId
        );

        assertEquals(
                MatchingStatus.ACTIVE,
                result.getStatus()
        );

        assertEquals(
                socialWorkerId,
                result.getSocialWorkerId()
        );

        assertNotNull(
                result.getAssignedAt()
        );
    }

    @Test
    @DisplayName("매칭 실패 시 FAILED 상태로 전환한다")
    void failMatching() {

        SocialWorkerMatchingResult result =
                SocialWorkerMatchingResult
                        .requested(
                                UUID.randomUUID()
                        );

        result.fail();

        assertEquals(
                MatchingStatus.FAILED,
                result.getStatus()
        );

        assertNull(
                result.getAssignedAt()
        );
    }
}