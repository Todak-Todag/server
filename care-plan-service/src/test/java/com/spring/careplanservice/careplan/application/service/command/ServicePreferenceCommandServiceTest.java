package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ServicePreferenceCommandServiceTest {
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID carePlanId = UUID.randomUUID();
    UUID planServiceId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();

    @Mock
    private CarePlanServiceQueryRepository carePlanServiceQueryRepository;

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Mock
    private ServicePreferenceCommandRepository servicePreferenceCommandRepository;

    @Spy
    private CarePlanOwnerValidator carePlanOwnerValidator;

    @InjectMocks
    private ServicePreferenceCommandService servicePreferenceCommandService;

    @Nested
    @DisplayName("서비스 희망 일정 생성")
    class CreateServicePreference {
        @Test
        @DisplayName("성공")
        void createServicePreference_success() {

        }

        @Test
        @DisplayName("Care Plan 서비스가 존재하지 않으면 예외")
        void createServicePreference_planService_notFound() {

        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void createServicePreference_carePlanNotFound() {

        }

        @Test
        @DisplayName("요청자가 Care Plan의 환자가 아니면 예외")
        void createServicePreference_forbidden() {

        }


    }
}