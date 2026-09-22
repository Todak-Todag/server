package com.todak_todag.schedule_service.schedule.infrastructure.client;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.FeignErrorCode;
import com.todak_todag.schedule_service.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;

// 상대 서비스가 2xx를 주고도 data/필드를 비워 보낸 경우를 걸러냄
//
// 응답 계약 위반은 재시도해도 결과가 같으므로 503이 아니라 502로 구분
// 두 Adapter가 같은 검사를 반복하지 않도록 여기에 모아둠
@Slf4j
public final class InternalApiResponses {

    private InternalApiResponses() {
    }

    public static <T> T requireData(ApiResponse<T> response, String callName) {
        return require(response == null ? null : response.data(), callName);
    }

    public static <T> T require(T value, String callName) {
        if (value == null) {
            log.error("[Schedule] 연계 서비스 응답에 필요한 값이 없습니다 call={}", callName);

            throw new BusinessException(FeignErrorCode.EXTERNAL_SERVICE_RESPONSE_INVALID);
        }

        return value;
    }
}
