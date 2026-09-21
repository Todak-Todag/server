package com.todak_todag.schedule_service.schedule.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.FeignErrorCode;
import com.todak_todag.schedule_service.schedule.infrastructure.client.care_plan.CarePlanClient;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.CarePlanRangeInternalResponse;
import feign.Feign;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import feign.codec.Decoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

// Feign 호출 실패가 어떤 예외로 변환되는지 검증한
//
// 예외 객체를 직접 만들어 던지는 대신 실제 HTTP 서버를 띄워 실제 Feign 호출을 태움
// downstream status가 예외 타입으로 바뀌는 지점이 Feign 내부라서, 그 경로를 그대로 지나야
// "ErrorDecoder를 타는 경우 / 타지 못하는 경우(연결 실패·타임아웃·디코딩)"가 구분되기 때문
class InternalApiErrorDecoderTest {

    private static final String SERVICE_PREFERENCE_PATH_ID = "11111111-1111-4111-8111-111111111111";

    private HttpServer server;

    // 스텁이 돌려줄 응답 — 테스트마다 바꿔 끼움
    private final AtomicInteger responseStatus = new AtomicInteger(HttpStatus.OK.value());
    private final AtomicReference<String> responseBody = new AtomicReference<>("{}");
    private final AtomicLong responseDelayMillis = new AtomicLong(0L);

    private CarePlanClient carePlanClient;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                if (responseDelayMillis.get() > 0) {
                    Thread.sleep(responseDelayMillis.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus.get(), body.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        carePlanClient = clientTargeting("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    // 운영과 같은 조합(SpringMvcContract + InternalApiErrorDecoder)으로 실제 Feign 프록시를 만듦
    private CarePlanClient clientTargeting(String url) {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Decoder decoder = (response, type) ->
                objectMapper.readValue(response.body().asInputStream(), objectMapper.constructType(type));

        return Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(decoder)
                .errorDecoder(new InternalApiErrorDecoder())
                // 연결 1초 / 응답 300ms — 타임아웃 케이스를 테스트 시간 안에 재현하기 위한 값
                .options(new Request.Options(1_000, TimeUnit.MILLISECONDS, 300, TimeUnit.MILLISECONDS, true))
                .target(CarePlanClient.class, url);
    }

    private void stub(int status, String body) {
        responseStatus.set(status);
        responseBody.set(body);
    }

    @Test
    @DisplayName("downstream이 404를 반환하면 403 AUTH_FORBIDDEN으로 변환한다")
    void decode_notFound_toForbidden() {
        // given
        stub(HttpStatus.NOT_FOUND.value(), """
                {"success":false,"code":"SERVICE_PREFERENCE_NOT_FOUND","message":"존재하지 않는 희망 일정입니다."}
                """);

        // when
        BusinessException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                BusinessException.class
        );

        // then — 소유권 검증 중 발생하므로 리소스 존재 여부를 노출하지 않음
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
        assertThat(exception.getErrorCode().getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("downstream이 400을 반환하면 EXTERNAL_SERVICE_CALL_REJECTED(500)로 변환한다")
    void decode_badRequest_toCallRejected() {
        // given
        stub(HttpStatus.BAD_REQUEST.value(), "{\"success\":false}");

        // when
        BusinessException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                BusinessException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_CALL_REJECTED);
        assertThat(exception.getErrorCode().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("downstream이 401을 반환하면 EXTERNAL_SERVICE_CALL_REJECTED(500)로 변환한다")
    void decode_unauthorized_toCallRejected() {
        // given — 내부 API 키 불일치. 최종 사용자의 인증 문제가 아니라 schedule-service 설정 결함
        stub(HttpStatus.UNAUTHORIZED.value(), "{\"success\":false}");

        // when
        BusinessException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                BusinessException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_CALL_REJECTED);
    }

    @Test
    @DisplayName("downstream이 403을 반환하면 EXTERNAL_SERVICE_CALL_REJECTED(500)로 변환한다")
    void decode_forbidden_toCallRejected() {
        // given
        stub(HttpStatus.FORBIDDEN.value(), "{\"success\":false}");

        // when
        BusinessException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                BusinessException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_CALL_REJECTED);
    }

    @Test
    @DisplayName("downstream이 500을 반환하면 EXTERNAL_SERVICE_UNAVAILABLE(503)로 변환한다")
    void decode_internalServerError_toUnavailable() {
        // given
        stub(HttpStatus.INTERNAL_SERVER_ERROR.value(), "{\"success\":false}");

        // when
        BusinessException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                BusinessException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        assertThat(exception.getErrorCode().getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("downstream이 503을 반환하면 EXTERNAL_SERVICE_UNAVAILABLE(503)로 변환한다")
    void decode_serviceUnavailable_toUnavailable() {
        // given
        stub(HttpStatus.SERVICE_UNAVAILABLE.value(), "{\"success\":false}");

        // when
        BusinessException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                BusinessException.class
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("downstream 오류 응답 본문은 예외 메시지로 새어 나가지 않는다")
    void decode_doesNotLeakDownstreamBody() {
        // given — 상대 서비스의 내부 구현이 드러나는 본문
        stub(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "{\"message\":\"could not execute statement [ERROR: relation \\\"care_plan_schema.p_care_plans\\\"]\"}");

        // when
        BusinessException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                BusinessException.class
        );

        // then — 자체 ErrorCode 문구만 남는다
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage())
                .isEqualTo(FeignErrorCode.EXTERNAL_SERVICE_UNAVAILABLE.getMessage())
                .doesNotContain("p_care_plans");
    }

    @Test
    @DisplayName("대상 서비스에 연결할 수 없으면 status가 없는 RetryableException이 발생한다")
    void call_connectionRefused_toRetryableException() throws IOException {
        // given — 아무도 듣고 있지 않은 포트
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        CarePlanClient unreachableClient = clientTargeting("http://127.0.0.1:" + closedPort);

        // when
        FeignException exception = catchThrowableOfType(
                () -> unreachableClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                FeignException.class
        );

        // then — HTTP 응답이 없어 ErrorDecoder를 타지 못하고, status로도 구분할 수 없음
        //        GlobalExceptionHandler가 이 status(<=0)를 503으로 변환
        assertThat(exception).isInstanceOf(RetryableException.class);
        assertThat(exception.status()).isLessThanOrEqualTo(0);
    }

    @Test
    @DisplayName("읽기 타임아웃이 나면 status가 없는 RetryableException이 발생한다")
    void call_readTimeout_toRetryableException() {
        // given — 응답 타임아웃(300ms)보다 오래 끄는 스텁
        stub(HttpStatus.OK.value(), "{}");
        responseDelayMillis.set(800L);

        // when
        FeignException exception = catchThrowableOfType(
                () -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)),
                FeignException.class
        );

        // then
        assertThat(exception).isInstanceOf(RetryableException.class);
        assertThat(exception.status()).isLessThanOrEqualTo(0);
    }

    @Test
    @DisplayName("2xx인데 본문을 해석할 수 없으면 status가 2xx인 FeignException이 발생한다")
    void call_undecodableBody_toDecodeException() {
        // given
        stub(HttpStatus.OK.value(), "not-a-json");

        // when & then — ErrorDecoder는 non-2xx만 다루므로 여기로 오지 않음
        //               GlobalExceptionHandler가 이 status(2xx)를 502로 변환
        assertThatThrownBy(() -> carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)))
                .isInstanceOf(FeignException.class)
                .extracting(e -> ((FeignException) e).status())
                .isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("downstream이 정상 응답하면 기존과 동일하게 값을 그대로 반환한다")
    void call_success_unchanged() {
        // given
        UUID carePlanId = UUID.fromString("22222222-2222-4222-8222-222222222222");
        UUID patientId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        stub(HttpStatus.OK.value(), """
                {"success":true,"code":200,"message":"조회 성공",
                 "data":{"carePlanId":"%s","finishDate":"2026-09-30","patientId":"%s"}}
                """.formatted(carePlanId, patientId));

        // when
        CarePlanRangeInternalResponse response =
                carePlanClient.findCarePlanRange(UUID.fromString(SERVICE_PREFERENCE_PATH_ID)).data();

        // then
        assertThat(response.carePlanId()).isEqualTo(carePlanId);
        assertThat(response.patientId()).isEqualTo(patientId);
        assertThat(response.finishDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }
}
