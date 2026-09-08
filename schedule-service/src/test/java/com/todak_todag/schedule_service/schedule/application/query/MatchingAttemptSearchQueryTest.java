package com.todak_todag.schedule_service.schedule.application.query;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingAttemptSearchQueryTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Nested
    @DisplayName("status 기본값 보정")
    class statusDefaultTest {

        @Test
        void status가_null이면_기본값_FAILED로_보정한다() {
            // given
            UUID userId = UUID.randomUUID();

            // when
            MatchingAttemptSearchQuery query = MatchingAttemptSearchQuery.of(userId, null, PAGEABLE);

            // then
            assertThat(query.status()).isEqualTo(MatchingAttemptStatus.FAILED);
        }

        @Test
        void status가_지정되면_그대로_사용한다() {
            // given
            UUID userId = UUID.randomUUID();

            // when
            MatchingAttemptSearchQuery query =
                    MatchingAttemptSearchQuery.of(userId, MatchingAttemptStatus.MATCHED, PAGEABLE);

            // then
            assertThat(query.status()).isEqualTo(MatchingAttemptStatus.MATCHED);
        }

        @Test
        void userId와_pageable은_그대로_전달된다() {
            // given
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(2, 30);

            // when
            MatchingAttemptSearchQuery query = MatchingAttemptSearchQuery.of(userId, null, pageable);

            // then
            assertThat(query.userId()).isEqualTo(userId);
            assertThat(query.pageable()).isEqualTo(pageable);
        }
    }

    @Nested
    @DisplayName("일정 미생성 조건 적용 여부")
    class excludeAlreadyScheduledTest {

        @Test
        void FAILED_조회면_일정_미생성_조건을_적용한다() {
            // given
            MatchingAttemptSearchQuery query =
                    MatchingAttemptSearchQuery.of(UUID.randomUUID(), MatchingAttemptStatus.FAILED, PAGEABLE);

            // when
            boolean excludeAlreadyScheduled = query.excludeAlreadyScheduled();

            // then
            assertThat(excludeAlreadyScheduled).isTrue();
        }

        @Test
        void status_미지정으로_FAILED가_적용된_경우에도_일정_미생성_조건을_적용한다() {
            // given
            MatchingAttemptSearchQuery query =
                    MatchingAttemptSearchQuery.of(UUID.randomUUID(), null, PAGEABLE);

            // when
            boolean excludeAlreadyScheduled = query.excludeAlreadyScheduled();

            // then
            assertThat(excludeAlreadyScheduled).isTrue();
        }

        @Test
        void MATCHED_조회면_일정_미생성_조건을_적용하지_않는다() {
            // given
            // MATCHED 시도는 성공과 함께 반드시 일정이 생성되므로 같은 조건을 걸면 결과가 항상 비게 된다
            MatchingAttemptSearchQuery query =
                    MatchingAttemptSearchQuery.of(UUID.randomUUID(), MatchingAttemptStatus.MATCHED, PAGEABLE);

            // when
            boolean excludeAlreadyScheduled = query.excludeAlreadyScheduled();

            // then
            assertThat(excludeAlreadyScheduled).isFalse();
        }
    }
}
