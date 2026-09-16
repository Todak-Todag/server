package com.todak_todag.discharge_service.discharge.domain.entity;

import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DischargeTest {

    private Discharge createScheduledDischarge() {
        return Discharge.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Hospital",
                LocalDate.now().plusDays(3)
        );
    }

    @Test
    void 퇴원건은_SCHEDULED_상태로_생성된다() {
        Discharge discharge =
                createScheduledDischarge();

        assertThat(discharge.getStatus())
                .isEqualTo(DischargeStatus.SCHEDULED);
    }

    @Test
    void 예정된_퇴원건을_연기할_수_있다() {
        Discharge discharge =
                createScheduledDischarge();

        LocalDate changedScheduledDate =
                LocalDate.now().plusDays(7);

        discharge.update(
                DischargeStatus.POSTPONED,
                changedScheduledDate
        );

        assertThat(discharge.getStatus())
                .isEqualTo(DischargeStatus.POSTPONED);

        assertThat(discharge.getScheduledDate())
                .isEqualTo(changedScheduledDate);
    }

    @Test
    void 예정된_퇴원건을_취소할_수_있다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.update(
                DischargeStatus.CANCELED,
                null
        );

        assertThat(discharge.getStatus())
                .isEqualTo(DischargeStatus.CANCELED);
    }

    @Test
    void 연기된_퇴원건을_취소할_수_있다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.update(
                DischargeStatus.POSTPONED,
                LocalDate.now().plusDays(7)
        );

        discharge.update(
                DischargeStatus.CANCELED,
                null
        );

        assertThat(discharge.getStatus())
                .isEqualTo(DischargeStatus.CANCELED);
    }

    @Test
    void 예정된_퇴원건을_완료할_수_있다() {
        Discharge discharge =
                createScheduledDischarge();

        LocalDate actualDate =
                LocalDate.now();

        discharge.complete(actualDate);

        assertThat(discharge.getStatus())
                .isEqualTo(DischargeStatus.COMPLETED);

        assertThat(discharge.getActualDate())
                .isEqualTo(actualDate);
    }

    @Test
    void 연기된_퇴원건을_완료할_수_있다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.update(
                DischargeStatus.POSTPONED,
                LocalDate.now().plusDays(7)
        );

        LocalDate actualDate =
                LocalDate.now();

        discharge.complete(actualDate);

        assertThat(discharge.getStatus())
                .isEqualTo(DischargeStatus.COMPLETED);

        assertThat(discharge.getActualDate())
                .isEqualTo(actualDate);
    }

    @Test
    void 연기된_퇴원건을_예정_상태로_되돌릴_수_없다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.update(
                DischargeStatus.POSTPONED,
                LocalDate.now().plusDays(7)
        );

        assertThatThrownBy(
                () -> discharge.update(
                        DischargeStatus.SCHEDULED,
                        LocalDate.now().plusDays(8)
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION
                                    );
                        }
                );
    }

    @Test
    void 완료된_퇴원건은_수정할_수_없다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.complete(
                LocalDate.now()
        );

        assertThatThrownBy(
                () -> discharge.update(
                        null,
                        LocalDate.now().plusDays(7)
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION
                                    );
                        }
                );
    }

    @Test
    void 취소된_퇴원건은_수정할_수_없다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.update(
                DischargeStatus.CANCELED,
                null
        );

        assertThatThrownBy(
                () -> discharge.update(
                        null,
                        LocalDate.now().plusDays(7)
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION
                                    );
                        }
                );
    }

    @Test
    void 완료된_퇴원건은_다시_완료할_수_없다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.complete(
                LocalDate.now()
        );

        assertThatThrownBy(
                () -> discharge.complete(
                        LocalDate.now()
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION
                                    );
                        }
                );
    }

    @Test
    void 취소된_퇴원건은_완료할_수_없다() {
        Discharge discharge =
                createScheduledDischarge();

        discharge.update(
                DischargeStatus.CANCELED,
                null
        );

        assertThatThrownBy(
                () -> discharge.complete(
                        LocalDate.now()
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_STATUS_TRANSITION
                                    );
                        }
                );
    }
}