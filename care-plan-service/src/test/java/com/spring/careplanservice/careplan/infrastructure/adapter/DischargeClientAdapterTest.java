package com.spring.careplanservice.careplan.infrastructure.adapter;

import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.infrastructure.client.DischargeFeignClient;
import com.spring.careplanservice.careplan.infrastructure.client.DischargeInternalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class DischargeClientAdapterTest {
    UUID dischargeId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    @Mock
    private DischargeFeignClient dischargeFeignClient;

    @InjectMocks
    private DischargeClientAdapter dischargeClientAdapter;

    @Test
    @DisplayName("퇴원 건 내부 API 응답을 조회 결과로 변환")
    void findById_success() {
        LocalDate actualDate = LocalDate.of(2026, 8, 1);

        DischargeInternalResponse dischargeInternalResponse = new DischargeInternalResponse(
                true,
                200,
                "퇴원 건 조회 성공",
                new DischargeInternalResponse.Data(
                        dischargeId,
                        patientId,
                        actualDate
                )
        );

        given(dischargeFeignClient.findById(dischargeId)).willReturn(dischargeInternalResponse);

        DischargeFindResult result = dischargeClientAdapter.findById(dischargeId);

        assertThat(result.dischargeId()).isEqualTo(dischargeId);
        assertThat(result.patientId()).isEqualTo(patientId);
        assertThat(result.actualDate()).isEqualTo(actualDate);

        verify(dischargeFeignClient).findById(dischargeId);
    }
}