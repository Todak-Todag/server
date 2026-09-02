package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;


@ExtendWith(MockitoExtension.class)
class CarePlanCommandServiceTest {
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Mock
    private CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @InjectMocks
    private CarePlanCommandService carePlanCommandService;

    @Nested
    @DisplayName("Care Plan 생성")
    class CreateCarePlan {
        @Test
        @DisplayName("실제 퇴원일 다음날부터 30일의 Care Plan 생성")
        void createCarePlan_success() {

        }

        @Test
        @DisplayName("서비스를 선택하지 않아도 Care Plan 생성 가능")
        void createCarePlan_noService_success() {
        }

        @Test
        @DisplayName("동일한 퇴원 건으로 생성 시 예외")
        void createCarePlan_duplicate(){

        }
        
        @Test
        @DisplayName("퇴원 건의 환자와 요청 환자가 다르면 예외")
        void createCarePlan_patientMismatch(){

        }
        
        @Test
        @DisplayName("실제 퇴원일이 없으면 Care Plan 예외")
        void createCarePlan_actualDateNull(){

        }
    }
}