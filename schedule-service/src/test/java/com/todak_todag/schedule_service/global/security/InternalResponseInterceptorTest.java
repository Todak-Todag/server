package com.todak_todag.schedule_service.global.security;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// InternalResponseInterceptor는 /internal/v1/** 전체(06/10번 API 등)에 공통 적용되므로,
// 특정 Controller가 아닌 Interceptor 자체의 동작을 여기서 별도로 검증한다.
// preHandle 분기(누락/공백/불일치/일치)는 MockHttpServletRequest 기반 단위 테스트로,
// 실제 /internal/v1/** 경로 등록과 401 응답 포맷은 MockMvc 통합 테스트로 나눠 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalResponseInterceptorTest {

    private static final String URI = "/internal/v1/service-results/{serviceResultId}";

    @Autowired
    private MockMvc mockMvc;

    // src/test/resources/application.yaml의 internal.key — 값이 바뀌어도 테스트가 따라가도록 주입받는다
    @Value("${internal.key}")
    private String internalKey;

    @Test
    void internal_key_설정이_없으면_생성_시점에_예외가_발생한다() {
        // given & when & then
        assertThatThrownBy(() -> new InternalResponseInterceptor(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new InternalResponseInterceptor("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 헤더가_없으면_UNAUTHORIZED_INTERNAL_REQUEST_예외가_발생한다() {
        // given
        InternalResponseInterceptor interceptor = new InternalResponseInterceptor(internalKey);
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
    }

    @Test
    void 헤더가_공백_문자열이면_UNAUTHORIZED_INTERNAL_REQUEST_예외가_발생한다() {
        // given
        InternalResponseInterceptor interceptor = new InternalResponseInterceptor(internalKey);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(InternalHeader.INTERNAL_KEY, "   ");

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
    }

    @Test
    void 헤더_값이_일치하지_않으면_UNAUTHORIZED_INTERNAL_REQUEST_예외가_발생한다() {
        // given
        InternalResponseInterceptor interceptor = new InternalResponseInterceptor(internalKey);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(InternalHeader.INTERNAL_KEY, "wrong-key");

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
    }

    @Test
    void 헤더_값의_길이가_달라도_예외로_처리한다() {
        // given
        // MessageDigest.isEqual은 길이가 다른 배열도 예외 없이 false를 반환한다 (상수 시간 비교)
        InternalResponseInterceptor interceptor = new InternalResponseInterceptor(internalKey);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(InternalHeader.INTERNAL_KEY, internalKey + "extra");

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
    }

    @Test
    void 헤더_값이_일치하면_true를_반환하고_통과시킨다() throws Exception {
        // given
        InternalResponseInterceptor interceptor = new InternalResponseInterceptor(internalKey);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(InternalHeader.INTERNAL_KEY, internalKey);

        // when
        boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then
        assertThat(passed).isTrue();
    }

    @Test
    void 내부_API_요청에_헤더가_없으면_401과_에러_응답을_반환한다() throws Exception {
        // given & when & then
        mockMvc.perform(get(URI, UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_REQUEST"));
    }

    @Test
    void 내부_API_요청의_헤더_값이_일치하지_않으면_401과_에러_응답을_반환한다() throws Exception {
        // given & when & then
        mockMvc.perform(get(URI, UUID.randomUUID())
                        .header(InternalHeader.INTERNAL_KEY, "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_REQUEST"));
    }

    @Test
    void 내부_API_요청의_헤더_값이_일치하면_Interceptor를_통과해_Controller까지_도달한다() throws Exception {
        // given
        // 존재하지 않는 ID이므로 Controller 로직에 의해 404가 반환된다 — 401이 아니라는 점이 Interceptor 통과의 증거
        UUID notExistingId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get(URI, notExistingId)
                        .header(InternalHeader.INTERNAL_KEY, internalKey))
                .andExpect(status().isNotFound());
    }

    @Test
    void 외부_API_경로에는_Interceptor가_적용되지_않는다() throws Exception {
        // given
        // Interceptor는 /internal/v1/**에만 등록되므로, 외부 API는 헤더가 없어도 401이 아니어야 한다
        // (인증 주체가 없어 UserContext가 비므로 401 외의 다른 응답으로 처리된다)

        // when & then
        mockMvc.perform(get("/api/v1/service-schedules/{serviceScheduleId}", UUID.randomUUID()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }
}
