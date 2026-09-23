package com.todak_todag.schedule_service.schedule.infrastructure.client;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.FeignErrorCode;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// Internal API(Feign) 호출이 non-2xx를 받았을 때의 공통 변환 지점
//
// ErrorDecoder 한 곳에서 변환해 두 Feign Client(care-plan / provider)에 중복 구현이 생기지 않게 함
// 분기 기준은 downstream의 message 문자열이 아니라 HTTP status
// 상대 응답 본문은 내부 구현이 드러날 수 있어 응답에 싣지 않고, 자체 ErrorCode 문구만 내보냄
@Slf4j
@Component
public class InternalApiErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        // 404 = 없는 servicePreferenceId / serviceOfferingId
        // 이 호출들은 전부 소유권 검증 중에 일어나므로 "리소스 존재 여부 비노출" 정책에 맞춤
        if (status == HttpStatus.NOT_FOUND.value()) {
            log.warn("[Schedule] 연계 서비스에 대상 리소스 없음 method={} status={}", methodKey, status);

            return new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }

        // 상대 서버 오류. 잠시 후 재시도하면 성공할 수 있어 503으로 구분
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("[Schedule] 연계 서비스 서버 오류 method={} status={}", methodKey, status);

            return new BusinessException(FeignErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        // 400/401/403/422 등 상대가 우리 요청을 거부한 것이라 재시도해도 결과가 동일
        // API 호출자의 입력 문제가 아니므로 4xx로 돌려주지 않고 500으로 두되 코드로 구분
        if (status >= HttpStatus.BAD_REQUEST.value()) {
            log.error("[Schedule] 연계 서비스가 요청을 거부 method={} status={}", methodKey, status);

            return new BusinessException(FeignErrorCode.EXTERNAL_SERVICE_CALL_REJECTED);
        }

        // Feign은 non-2xx일 때만 여기로 오므로 도달할 일이 없음
        // 기본 동작을 바꾸지 않도록 위임
        return defaultDecoder.decode(methodKey, response);
    }
}
