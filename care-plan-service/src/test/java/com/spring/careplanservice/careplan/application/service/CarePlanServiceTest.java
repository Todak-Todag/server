package com.spring.careplanservice.careplan.application.service;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CarePlanServiceTest {
    UUID patientId = UUID.randomUUID();

    @Test
    @DisplayName("삭제하면 삭제 시각과 삭제 사용자가 기록된다")
    void delete() {
        CarePlanService carePlanService = CarePlanService.create(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        carePlanService.delete(patientId);

        assertThat(carePlanService.isDeleted()).isTrue();
        assertThat(carePlanService.getDeletedAt()).isNotNull();
        assertThat(carePlanService.getDeletedBy()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("이미 삭제된 서비스는 다시 삭제할 수 없다")
    void duplicateDelete() {
        CarePlanService carePlanService = CarePlanService.create(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        carePlanService.delete(patientId);

        assertThatThrownBy(() -> carePlanService.delete(patientId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 삭제된 엔티티입니다.");
    }
}
