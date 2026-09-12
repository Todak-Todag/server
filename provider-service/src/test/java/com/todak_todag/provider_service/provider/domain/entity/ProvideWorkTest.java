package com.todak_todag.provider_service.provider.domain.entity;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.UUID;

@DisplayName("제공 가능 일정")
class ProvideWorkTest {

    private static final UUID SERVICE_OFFERING_ID = UUID.randomUUID();
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime THIRTEEN = LocalTime.of(13, 0);

    @Test
    @DisplayName("요일이 1(월)이면 등록된다")
    void of_monday() {
        assertThat(ProvideWork.of(SERVICE_OFFERING_ID, 1, NINE, THIRTEEN).getDay()).isEqualTo(1);
    }

    @Test
    @DisplayName("요일이 7(일)이면 등록된다")
    void of_sunday() {
        assertThat(ProvideWork.of(SERVICE_OFFERING_ID, 7, NINE, THIRTEEN).getDay()).isEqualTo(7);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 8, -1})
    @DisplayName("요일이 1~7을 벗어나면 PROVIDE_WORK_INVALID_DAY")
    void of_invalidDay(int day) {
        assertThatThrownBy(() -> ProvideWork.of(SERVICE_OFFERING_ID, day, NINE, THIRTEEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ProviderErrorCode.PROVIDE_WORK_INVALID_DAY);
    }

    @Test
    @DisplayName("요일이 없으면 PROVIDE_WORK_INVALID_DAY")
    void of_nullDay() {
        assertThatThrownBy(() -> ProvideWork.of(SERVICE_OFFERING_ID, null, NINE, THIRTEEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ProviderErrorCode.PROVIDE_WORK_INVALID_DAY);
    }

    @Test
    @DisplayName("수정할 때도 요일 범위를 검증한다")
    void update_invalidDay() {
        ProvideWork provideWork = ProvideWork.of(SERVICE_OFFERING_ID, 1, NINE, THIRTEEN);

        assertThatThrownBy(() -> provideWork.update(0, NINE, THIRTEEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ProviderErrorCode.PROVIDE_WORK_INVALID_DAY);
    }
}