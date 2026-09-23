package com.todak_todag.discharge_service.discharge.application.service.query;

import com.todak_todag.discharge_service.discharge.application.query.DischargeSearchQuery;
import com.todak_todag.discharge_service.discharge.application.result.DischargeFindResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeSearchResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import com.todak_todag.discharge_service.discharge.domain.repository.query.DischargeQueryRepository;
import com.todak_todag.discharge_service.global.common.UserRole;
import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DischargeQueryServiceTest {

    @Mock
    private DischargeQueryRepository dischargeQueryRepository;

    private DischargeQueryService dischargeQueryService;

    @BeforeEach
    void setUp() {
        dischargeQueryService =
                new DischargeQueryService(dischargeQueryRepository);
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

        when(dischargeQueryRepository.findById(dischargeId))
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

        when(dischargeQueryRepository.findById(dischargeId))
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

        when(dischargeQueryRepository.findById(dischargeId))
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

        when(dischargeQueryRepository.findById(dischargeId))
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

        when(dischargeQueryRepository.findById(dischargeId))
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

        when(dischargeQueryRepository.findById(dischargeId))
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

    @Test
    void 퇴원건_목록_조회_결과를_SearchResult로_매핑해_반환한다() {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        LocalDate scheduledDate =
                LocalDate.of(2026, 9, 10);

        Pageable pageable =
                PageRequest.of(0, 10);

        Discharge discharge = mock(Discharge.class);

        when(discharge.getId())
                .thenReturn(dischargeId);
        when(discharge.getPatientId())
                .thenReturn(patientId);
        when(discharge.getHospitalName())
                .thenReturn("Test Hospital");
        when(discharge.getStatus())
                .thenReturn(DischargeStatus.SCHEDULED);
        when(discharge.getScheduledDate())
                .thenReturn(scheduledDate);
        when(discharge.getActualDate())
                .thenReturn(null);

        DischargeSearchQuery query =
                new DischargeSearchQuery(
                        hospitalStaffId,
                        DischargeStatus.SCHEDULED,
                        scheduledDate,
                        pageable
                );

        when(
                dischargeQueryRepository.search(
                        hospitalStaffId,
                        DischargeStatus.SCHEDULED,
                        scheduledDate,
                        pageable
                )
        )
                .thenReturn(
                        new PageImpl<>(
                                List.of(discharge),
                                pageable,
                                1
                        )
                );

        var result =
                dischargeQueryService.searchDischarges(query);

        assertThat(result.getTotalElements())
                .isEqualTo(1);

        DischargeSearchResult content =
                result.getContent().get(0);

        assertThat(content.dischargeId())
                .isEqualTo(dischargeId);
        assertThat(content.patientId())
                .isEqualTo(patientId);
        assertThat(content.hospitalName())
                .isEqualTo("Test Hospital");
        assertThat(content.status())
                .isEqualTo(DischargeStatus.SCHEDULED);
        assertThat(content.scheduledDate())
                .isEqualTo(scheduledDate);
        assertThat(content.actualDate())
                .isNull();

        verify(dischargeQueryRepository)
                .search(
                        hospitalStaffId,
                        DischargeStatus.SCHEDULED,
                        scheduledDate,
                        pageable
                );
    }

    @Test
    void 퇴원건_목록_조회_결과가_없으면_빈_페이지를_반환한다() {
        UUID hospitalStaffId = UUID.randomUUID();

        Pageable pageable =
                PageRequest.of(0, 10);

        DischargeSearchQuery query =
                new DischargeSearchQuery(
                        hospitalStaffId,
                        null,
                        null,
                        pageable
                );

        when(
                dischargeQueryRepository.search(
                        hospitalStaffId,
                        null,
                        null,
                        pageable
                )
        )
                .thenReturn(
                        new PageImpl<>(
                                List.of(),
                                pageable,
                                0
                        )
                );

        var result =
                dischargeQueryService.searchDischarges(query);

        assertThat(result.getContent())
                .isEmpty();

        assertThat(result.getTotalElements())
                .isZero();

        verify(dischargeQueryRepository)
                .search(
                        hospitalStaffId,
                        null,
                        null,
                        pageable
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