package com.todak_todag.discharge_service.global.security;

import com.todak_todag.discharge_service.global.exception.BusinessException;
import com.todak_todag.discharge_service.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiKeyInterceptorTest {

    private static final String INTERNAL_API_KEY =
            "test-internal-api-key";

    private InternalApiKeyInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor =
                new InternalApiKeyInterceptor(
                        INTERNAL_API_KEY
                );
    }

    @Test
    void 올바른_내부_API_Key이면_요청을_허용한다() {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        request.addHeader(
                InternalHeader.INTERNAL_KEY,
                INTERNAL_API_KEY
        );

        boolean result =
                interceptor.preHandle(
                        request,
                        response,
                        new Object()
                );

        assertThat(result).isTrue();
    }

    @Test
    void 내부_API_Key가_없으면_인증에_실패한다() {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        assertThatThrownBy(
                () -> interceptor.preHandle(
                        request,
                        response,
                        new Object()
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(
                                    businessException.getErrorCode()
                            )
                                    .isEqualTo(
                                            ErrorCode.AUTH_UNAUTHORIZED_INTERNAL_REQUEST
                                    );
                        }
                );
    }

    @Test
    void 잘못된_내부_API_Key이면_인증에_실패한다() {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        request.addHeader(
                InternalHeader.INTERNAL_KEY,
                "wrong-api-key"
        );

        assertThatThrownBy(
                () -> interceptor.preHandle(
                        request,
                        response,
                        new Object()
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        exception -> {
                            BusinessException businessException =
                                    (BusinessException) exception;

                            assertThat(
                                    businessException.getErrorCode()
                            )
                                    .isEqualTo(
                                            ErrorCode.AUTH_UNAUTHORIZED_INTERNAL_REQUEST
                                    );
                        }
                );
    }

    @Test
    void internal_key가_비어있으면_Interceptor_생성에_실패한다() {
        assertThatThrownBy(
                () -> new InternalApiKeyInterceptor("")
        )
                .isInstanceOf(IllegalArgumentException.class);
    }
}