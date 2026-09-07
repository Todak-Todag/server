package com.spring.careplanservice.careplan.infrastructure.messaging;

class CarePlanCompletedEventConsumeIntegrationTest {
    /*
    *
     COMPLETED + serviceResultId 존재 이벤트 수신 → CarePlan COMPLETED
    CANCELED + serviceResultId null 이벤트 수신 → CarePlan COMPLETED
    잘못된 payload 처리 정책이 있다면 그 케이스
    */

}