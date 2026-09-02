package com.todak_todag.discharge_service.discharge.application.command_service;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.repository.DischargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DischargeCommandServiceTest {

    @Mock
    private DischargeRepository dischargeRepository;

    private DischargeCommandService dischargeCommandService;

    @BeforeEach
    void setUp() {
        dischargeCommandService =
                new DischargeCommandService(dischargeRepository);
    }

    @Test
    void 퇴원건을_생성한다() {
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

        when(dischargeRepository.save(any(Discharge.class)))
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
}