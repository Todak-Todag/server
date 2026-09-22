package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.FeignErrorCode;
import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.infrastructure.client.care_plan.CarePlanClient;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanRangeInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.ProviderServiceOfferingInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.ServiceOfferingIdListInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.provider.ProviderServiceOfferingClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

// 상대 서비스가 2xx를 주고도 본문을 비워 보낸 경우(응답 계약 위반)를 검증
@ExtendWith(MockitoExtension.class)
class InternalApiResponseContractTest {

    @Nested
    @DisplayName("care-plan-service 호출")
    class CarePlanAdapterTest {

        @Mock
        private CarePlanClient carePlanClient;

        @InjectMocks
        private CarePlanAdapter carePlanAdapter;

        @Test
        @DisplayName("응답 data가 비어 있으면 EXTERNAL_SERVICE_RESPONSE_INVALID(502)를 던진다")
        void findCarePlanRange_nullData_throwsResponseInvalid() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();
            given(carePlanClient.findCarePlanRange(servicePreferenceId))
                    .willReturn(ApiResponse.ok("조회 성공", null));

            // when & then
            assertThatThrownBy(() -> carePlanAdapter.findCarePlanRange(servicePreferenceId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_RESPONSE_INVALID);

            assertThat(FeignErrorCode.EXTERNAL_SERVICE_RESPONSE_INVALID.getStatus())
                    .isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        @DisplayName("정상 응답이면 기존과 동일하게 CarePlanRange로 변환한다")
        void findCarePlanRange_success_unchanged() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();
            UUID carePlanId = UUID.randomUUID();
            UUID patientId = UUID.randomUUID();
            LocalDate finishDate = LocalDate.of(2026, 9, 30);

            given(carePlanClient.findCarePlanRange(servicePreferenceId)).willReturn(
                    ApiResponse.ok("조회 성공", new CarePlanRangeInternalResponse(carePlanId, finishDate, patientId))
            );

            // when
            CarePlanPort.CarePlanRange result = carePlanAdapter.findCarePlanRange(servicePreferenceId);

            // then
            assertThat(result.carePlanId()).isEqualTo(carePlanId);
            assertThat(result.finishDate()).isEqualTo(finishDate);
            assertThat(result.patientId()).isEqualTo(patientId);
        }
    }

    @Nested
    @DisplayName("provider-service 호출")
    class ProviderOfferingAdapterTest {

        @Mock
        private ProviderServiceOfferingClient providerServiceOfferingClient;

        @InjectMocks
        private ProviderOfferingAdapter providerOfferingAdapter;

        @Test
        @DisplayName("응답 data의 providerId가 비어 있으면 EXTERNAL_SERVICE_RESPONSE_INVALID(502)를 던진다")
        void findAssignedProviderId_nullField_throwsResponseInvalid() {
            // given
            UUID serviceOfferingId = UUID.randomUUID();
            given(providerServiceOfferingClient.findServiceOffering(serviceOfferingId)).willReturn(
                    ApiResponse.ok("조회 성공", new ProviderServiceOfferingInternalResponse(null))
            );

            // when & then
            assertThatThrownBy(() -> providerOfferingAdapter.findAssignedProviderId(serviceOfferingId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_RESPONSE_INVALID);
        }

        @Test
        @DisplayName("정상 응답이면 기존과 동일하게 목록을 그대로 반환한다")
        void findServiceOfferingIds_success_unchanged() {
            // given
            UUID providerId = UUID.randomUUID();
            List<UUID> serviceOfferingIds = List.of(UUID.randomUUID(), UUID.randomUUID());

            given(providerServiceOfferingClient.findServiceOfferingIds(providerId)).willReturn(
                    ApiResponse.ok("조회 성공", new ServiceOfferingIdListInternalResponse(serviceOfferingIds))
            );

            // when
            List<UUID> result = providerOfferingAdapter.findServiceOfferingIds(providerId);

            // then
            assertThat(result).isEqualTo(serviceOfferingIds);
        }
    }
}
