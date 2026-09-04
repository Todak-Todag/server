package com.todak_todag.discharge_service.discharge.application.service.query;

import com.todak_todag.discharge_service.discharge.application.result.DischargeFindResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.DischargeRepository;
import com.todak_todag.discharge_service.global.common.UserRole;
import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DischargeQueryServiceTest {

    @Mock
    private DischargeRepository dischargeRepository;

    private DischargeQueryService dischargeQueryService;

    @BeforeEach
    void setUp() {
        dischargeQueryService =
                new DischargeQueryService(dischargeRepository);
    }

    @Test
    void 환자는_자신의_퇴원건을_조회할_수_있다() {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Instant createdAt =
                Instant.parse("2026-08-20T01:00:00Z");

        Discharge discharge =
                createDischargeMock(
                        dischargeId,
                        patientId,
                        hospitalStaffId,
                        createdAt
                );

        when(dischargeRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeFindResult result =
                dischargeQueryService.findDischarge(
                        dischargeId,
                        patientId,
                        UserRole.PATIENT
                );

        assertThat(result.dischargeId())
                .isEqualTo(dischargeId);
        assertThat(result.patientId())
                .isEqualTo(patientId);
        assertThat(result.hospitalStaffId())
                .isEqualTo(hospitalStaffId);
        assertThat(result.createdAt())
                .isEqualTo(createdAt);
    }

    @Test
    void 다른_환자의_퇴원건은_조회할_수_없다() {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getPatientId())
                .thenReturn(patientId);

        when(dischargeRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        assertThatThrownBy(
                () -> dischargeQueryService.findDischarge(
                        dischargeId,
                        otherPatientId,
                        UserRole.PATIENT
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
                        }
                );
    }

    @Test
    void 병원_담당자는_자신이_등록한_퇴원건을_조회할_수_있다() {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        Instant createdAt =
                Instant.parse("2026-08-20T01:00:00Z");

        Discharge discharge =
                createDischargeMock(
                        dischargeId,
                        patientId,
                        hospitalStaffId,
                        createdAt
                );

        when(dischargeRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        DischargeFindResult result =
                dischargeQueryService.findDischarge(
                        dischargeId,
                        hospitalStaffId,
                        UserRole.HOSPITAL_STAFF
                );

        assertThat(result.dischargeId())
                .isEqualTo(dischargeId);
        assertThat(result.patientId())
                .isEqualTo(patientId);
        assertThat(result.hospitalStaffId())
                .isEqualTo(hospitalStaffId);
        assertThat(result.createdAt())
                .isEqualTo(createdAt);
    }

    @Test
    void 다른_병원_담당자가_등록한_퇴원건은_조회할_수_없다() {
        UUID dischargeId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();
        UUID otherHospitalStaffId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);

        when(dischargeRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        assertThatThrownBy(
                () -> dischargeQueryService.findDischarge(
                        dischargeId,
                        otherHospitalStaffId,
                        UserRole.HOSPITAL_STAFF
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
                        }
                );
    }

    @Test
    void 허용되지_않은_역할은_퇴원건을_조회할_수_없다() {
        UUID dischargeId = UUID.randomUUID();

        Discharge discharge = mock(Discharge.class);

        when(dischargeRepository.findById(dischargeId))
                .thenReturn(Optional.of(discharge));

        assertThatThrownBy(
                () -> dischargeQueryService.findDischarge(
                        dischargeId,
                        UUID.randomUUID(),
                        UserRole.SOCIAL_WORKER
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
                        }
                );
    }

    @Test
    void 존재하지_않는_퇴원건은_조회할_수_없다() {
        UUID dischargeId = UUID.randomUUID();

        when(dischargeRepository.findById(dischargeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> dischargeQueryService.findDischarge(
                        dischargeId,
                        UUID.randomUUID(),
                        UserRole.PATIENT
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(businessException.getErrorCode())
                                    .isEqualTo(ErrorCode.DISCHARGE_NOT_FOUND);
                        }
                );
    }

    private Discharge createDischargeMock(
            UUID dischargeId,
            UUID patientId,
            UUID hospitalStaffId,
            Instant createdAt
    ) {
        Discharge discharge = mock(Discharge.class);

        when(discharge.getId())
                .thenReturn(dischargeId);
        when(discharge.getPatientId())
                .thenReturn(patientId);
        when(discharge.getHospitalStaffId())
                .thenReturn(hospitalStaffId);
        when(discharge.getHospitalName())
                .thenReturn("Test Hospital");
        when(discharge.getScheduledDate())
                .thenReturn(LocalDate.of(2026, 9, 10));
        when(discharge.getCreatedAt())
                .thenReturn(createdAt);

        return discharge;
    }
}