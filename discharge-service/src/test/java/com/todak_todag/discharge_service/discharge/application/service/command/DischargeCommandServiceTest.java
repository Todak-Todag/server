package com.todak_todag.discharge_service.discharge.application.service.command;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.command.DischargeUpdateCommand;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeUpdateResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import com.todak_todag.discharge_service.discharge.domain.repository.command.DischargeCommandRepository;
import com.todak_todag.discharge_service.discharge.domain.repository.query.DischargeQueryRepository;
import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DischargeCommandServiceTest {

    @Mock
    private DischargeCommandRepository dischargeCommandRepository;

    @Mock
    private DischargeQueryRepository dischargeQueryRepository;

    private DischargeCommandService dischargeCommandService;

    @BeforeEach
    void setUp() {
        dischargeCommandService =
                new DischargeCommandService(
                        dischargeCommandRepository,
                        dischargeQueryRepository
                );
    }

    @Test
    void 퇴원건을_생성한다() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();
        UUID dischargeId = UUID.randomUUID();

        DischargeCreateCommand command =
                new DischargeCreateCommand(
                        hospitalStaffId,
                        patientId,
                        "Test Hospital",
                        LocalDate.now().plusDays(1)
                );

        when(dischargeCommandRepository.save(any(Discharge.class)))
                .thenAnswer(invocation -> {
                    Discharge discharge = invocation.getArgument(0);

                    var idField =
                            Discharge.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(discharge, dischargeId);

                    return discharge;
                });

        DischargeCreateResult result =
                dischargeCommandService.createDischarge(command);

        assertThat(result.dischargeId())
                .isEqualTo(dischargeId);
    }

    @Test
    void 퇴원_예정일_수정에_성공한다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();
        LocalDate changedScheduledDate =
                LocalDate.now().plusDays(7);

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.SCHEDULED);
        when(discharge.getId())
                .thenReturn(dischargeId);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        null,
                        changedScheduledDate
                );

        DischargeUpdateResult result =
                dischargeCommandService.updateDischarge(command);

        verify(discharge)
                .update(
                        null,
                        changedScheduledDate
                );

        assertThat(result.dischargeId())
                .isEqualTo(dischargeId);
    }

    @Test
    void 예정된_퇴원건을_연기할_수_있다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();
        LocalDate changedScheduledDate =
                LocalDate.now().plusDays(7);

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.SCHEDULED);
        when(discharge.getId())
                .thenReturn(dischargeId);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        DischargeStatus.POSTPONED,
                        changedScheduledDate
                );

        DischargeUpdateResult result =
                dischargeCommandService.updateDischarge(command);

        verify(discharge)
                .update(
                        DischargeStatus.POSTPONED,
                        changedScheduledDate
                );

        assertThat(result.dischargeId())
                .isEqualTo(dischargeId);
    }

    @Test
    void 예정된_퇴원건을_취소할_수_있다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.SCHEDULED);
        when(discharge.getId())
                .thenReturn(dischargeId);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        DischargeStatus.CANCELED,
                        null
                );

        DischargeUpdateResult result =
                dischargeCommandService.updateDischarge(command);

        verify(discharge)
                .update(
                        DischargeStatus.CANCELED,
                        null
                );

        assertThat(result.dischargeId())
                .isEqualTo(dischargeId);
    }

    @Test
    void 연기된_퇴원건을_취소할_수_있다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.POSTPONED);
        when(discharge.getId())
                .thenReturn(dischargeId);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        DischargeStatus.CANCELED,
                        null
                );

        DischargeUpdateResult result =
                dischargeCommandService.updateDischarge(command);

        verify(discharge)
                .update(
                        DischargeStatus.CANCELED,
                        null
                );

        assertThat(result.dischargeId())
                .isEqualTo(dischargeId);
    }

    @Test
    void 존재하지_않는_퇴원건은_수정할_수_없다() {
        UUID dischargeId = UUID.randomUUID();

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.empty());

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        UUID.randomUUID(),
                        null,
                        LocalDate.now().plusDays(1)
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_NOT_FOUND
                                    );
                        }
                );
    }

    @Test
    void 다른_병원_담당자의_퇴원건은_수정할_수_없다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();
        UUID otherHospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        otherHospitalStaffId,
                        null,
                        LocalDate.now().plusDays(1)
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.AUTH_FORBIDDEN
                                    );
                        }
                );
    }

    @Test
    void 수정할_값이_하나도_없으면_수정할_수_없다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        null,
                        null
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_UPDATE_REQUEST
                                    );
                        }
                );
    }

    @Test
    void 퇴원_연기시_변경할_예정일은_필수이다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.SCHEDULED);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        DischargeStatus.POSTPONED,
                        null
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_SCHEDULED_DATE
                                    );
                        }
                );
    }

    @Test
    void 수정할_퇴원_예정일은_미래_날짜여야_한다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.SCHEDULED);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        null,
                        LocalDate.now()
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(
                                            ErrorCode.DISCHARGE_INVALID_SCHEDULED_DATE
                                    );
                        }
                );
    }

    @Test
    void 연기된_퇴원건을_예정_상태로_되돌릴_수_없다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.POSTPONED);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        DischargeStatus.SCHEDULED,
                        LocalDate.now().plusDays(1)
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
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
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.COMPLETED);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        null,
                        LocalDate.now().plusDays(1)
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
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
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.CANCELED);

        when(dischargeQueryRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        hospitalStaffId,
                        null,
                        LocalDate.now().plusDays(1)
                );

        assertThatThrownBy(
                () -> dischargeCommandService.updateDischarge(command)
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