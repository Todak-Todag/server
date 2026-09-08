package com.todak_todag.schedule_service.schedule.application.support;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchingAttemptValidatorTest {

    private static final LocalDate FINISH_DATE = LocalDate.of(2026, 9, 30);

    private final MatchingAttemptValidator matchingAttemptValidator = new MatchingAttemptValidator();

    private CarePlanPort.CarePlanRange carePlanRange() {
        return new CarePlanPort.CarePlanRange(UUID.randomUUID(), FINISH_DATE, UUID.randomUUID());
    }

    @Nested
    @DisplayName("재시도 가능 상태 검증")
    class validateRetryableTest {

        @Test
        @DisplayName("FAILED 상태면 통과한다")
        void FAILED면_통과한다() {
            // given
            MatchingAttemptStatus status = MatchingAttemptStatus.FAILED;

            // when & then
            assertThatCode(() -> matchingAttemptValidator.validateRetryable(status))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("MATCHED 상태면 409(MATCHING_ATTEMPT_NOT_RETRYABLE)를 던진다")
        void MATCHED면_409를_던진다() {
            // given
            MatchingAttemptStatus status = MatchingAttemptStatus.MATCHED;

            // when & then
            assertThatThrownBy(() -> matchingAttemptValidator.validateRetryable(status))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_NOT_RETRYABLE);
        }
    }

    @Nested
    @DisplayName("중복 접수 검증")
    class validateNotAlreadyRequestedTest {

        @Test
        @DisplayName("아웃박스에 적재된 적이 없으면 통과한다")
        void 적재된_적이_없으면_통과한다() {
            // given
            boolean alreadyRequested = false;

            // when & then
            assertThatCode(() -> matchingAttemptValidator.validateNotAlreadyRequested(alreadyRequested))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 적재됐으면 409(MATCHING_ATTEMPT_RETRY_ALREADY_REQUESTED)를 던진다")
        void 이미_적재됐으면_409를_던진다() {
            // given
            boolean alreadyRequested = true;

            // when & then
            assertThatThrownBy(() -> matchingAttemptValidator.validateNotAlreadyRequested(alreadyRequested))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_ALREADY_REQUESTED);
        }
    }

    @Nested
    @DisplayName("Care Plan 일정 범위 검증")
    class validateRetryDateTest {

        @Test
        @DisplayName("startDate는 finishDate에서 30일 고정 기간으로 역산된다")
        void startDate는_고정_기간으로_역산된다() {
            // given
            CarePlanPort.CarePlanRange range = carePlanRange();

            // when
            LocalDate startDate = range.startDate();

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 9, 1));
        }

        @Test
        @DisplayName("범위 안(경계 포함)의 날짜는 통과한다")
        void 범위_안이면_통과한다() {
            // given
            CarePlanPort.CarePlanRange range = carePlanRange();

            // when & then
            assertThatCode(() -> {
                matchingAttemptValidator.validateRetryDate(LocalDate.of(2026, 9, 1), range);
                matchingAttemptValidator.validateRetryDate(LocalDate.of(2026, 9, 10), range);
                matchingAttemptValidator.validateRetryDate(LocalDate.of(2026, 9, 30), range);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("startDate 이전이면 400을 던진다")
        void startDate_이전이면_400을_던진다() {
            // given
            CarePlanPort.CarePlanRange range = carePlanRange();
            LocalDate beforeStart = LocalDate.of(2026, 8, 31);

            // when & then
            assertThatThrownBy(() -> matchingAttemptValidator.validateRetryDate(beforeStart, range))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_EXCEEDS_CARE_PLAN_RANGE);
        }

        @Test
        @DisplayName("finishDate 이후면 400을 던진다")
        void finishDate_이후면_400을_던진다() {
            // given
            CarePlanPort.CarePlanRange range = carePlanRange();
            LocalDate afterFinish = LocalDate.of(2026, 10, 1);

            // when & then
            assertThatThrownBy(() -> matchingAttemptValidator.validateRetryDate(afterFinish, range))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_EXCEEDS_CARE_PLAN_RANGE);
        }
    }
}
