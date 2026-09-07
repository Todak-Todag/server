package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentErrorCode;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.ConsentCreateCommand;
import com.todak_todag.user_service.user.application.result.ConsentCreateResult;
import com.todak_todag.user_service.user.application.result.ConsentFindHistoryResult;
import com.todak_todag.user_service.user.application.service.command.ConsentCommandService;
import com.todak_todag.user_service.user.application.service.query.ConsentQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsentControllerTest {

    private static final String URI =
            "/api/v1/consents";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentCommandService consentCommandService;

    @MockitoBean
    private ConsentQueryService consentQueryService;

    @Nested
    @DisplayName("약관 동의")
    class CreateConsent {

        @Test
        @DisplayName("로그인 사용자가 여러 약관 버전 동의에 성공한다")
        void createConsent_success()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();

            UUID versionId1 = UUID.randomUUID();
            UUID versionId2 = UUID.randomUUID();

            UUID consentId1 = UUID.randomUUID();
            UUID consentId2 = UUID.randomUUID();

            setAuthentication(userId);

            ConsentCreateResult result =
                    new ConsentCreateResult(
                            List.of(
                                    consentId1,
                                    consentId2
                            )
                    );

            given(
                    consentCommandService.create(
                            any()
                    )
            ).willReturn(result);

            // when & then
            mockMvc.perform(
                            post(URI)
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentDocumentVersionIds": [
                                                "%s",
                                                "%s"
                                              ]
                                            }
                                            """.formatted(
                                                    versionId1,
                                                    versionId2
                                            )
                                    )
                    )
                    .andExpect(
                            status().isCreated()
                    )
                    .andExpect(
                            jsonPath("$.success")
                                    .value(true)
                    )
                    .andExpect(
                            jsonPath("$.code")
                                    .value(201)
                    )
                    .andExpect(
                            jsonPath("$.message")
                                    .value("약관 동의 성공")
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.consentIds.length()"
                            )
                                    .value(2)
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.consentIds[0]"
                            )
                                    .value(
                                            consentId1.toString()
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.consentIds[1]"
                            )
                                    .value(
                                            consentId2.toString()
                                    )
                    );

            ArgumentCaptor<ConsentCreateCommand> captor =
                    ArgumentCaptor.forClass(
                            ConsentCreateCommand.class
                    );

            then(consentCommandService)
                    .should()
                    .create(captor.capture());

            ConsentCreateCommand command =
                    captor.getValue();

            // userId는 Request Body가 아니라 인증 객체에서 전달되어야 한다.
            assertThat(command.userId())
                    .isEqualTo(userId);

            assertThat(
                    command.consentDocumentVersionIds()
            ).containsExactly(
                    versionId1,
                    versionId2
            );
        }

        @Test
        @DisplayName("동의할 약관 버전 목록이 비어 있으면 400을 반환한다")
        void createConsent_emptyVersionIds()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();

            setAuthentication(userId);

            // when & then
            mockMvc.perform(
                            post(URI)
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentDocumentVersionIds": []
                                            }
                                            """
                                    )
                    )
                    .andExpect(
                            status().isBadRequest()
                    );

            then(consentCommandService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("약관 버전 ID에 null이 포함되면 400을 반환한다")
        void createConsent_nullVersionId()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();

            setAuthentication(userId);

            // when & then
            mockMvc.perform(
                            post(URI)
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentDocumentVersionIds": [
                                                null
                                              ]
                                            }
                                            """
                                    )
                    )
                    .andExpect(
                            status().isBadRequest()
                    );

            then(consentCommandService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("중복된 약관 버전이 포함되면 400을 반환한다")
        void createConsent_duplicateVersion()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            setAuthentication(userId);

            given(
                    consentCommandService.create(
                            any()
                    )
            ).willThrow(
                    new BusinessException(
                            ConsentErrorCode
                                    .DUPLICATE_CONSENT_DOCUMENT_VERSION
                    )
            );

            // when & then
            mockMvc.perform(
                            post(URI)
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentDocumentVersionIds": [
                                                "%s",
                                                "%s"
                                              ]
                                            }
                                            """.formatted(
                                                    versionId,
                                                    versionId
                                            )
                                    )
                    )
                    .andExpect(
                            status().isBadRequest()
                    )
                    .andExpect(
                            jsonPath("$.success")
                                    .value(false)
                    )
                    .andExpect(
                            jsonPath(
                                    "$.error.errorCode"
                            )
                                    .value(
                                            "DUPLICATE_CONSENT_DOCUMENT_VERSION"
                                    )
                    );
        }

        @Test
        @DisplayName("동의할 수 없는 약관 버전이 포함되면 404를 반환한다")
        void createConsent_invalidVersion()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            setAuthentication(userId);

            given(
                    consentCommandService.create(
                            any()
                    )
            ).willThrow(
                    new BusinessException(
                            ConsentErrorCode
                                    .INVALID_CONSENT_DOCUMENT_VERSION
                    )
            );

            // when & then
            mockMvc.perform(
                            post(URI)
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentDocumentVersionIds": [
                                                "%s"
                                              ]
                                            }
                                            """.formatted(
                                                    versionId
                                            )
                                    )
                    )
                    .andExpect(
                            status().isNotFound()
                    )
                    .andExpect(
                            jsonPath("$.success")
                                    .value(false)
                    )
                    .andExpect(
                            jsonPath(
                                    "$.error.errorCode"
                            )
                                    .value(
                                            "INVALID_CONSENT_DOCUMENT_VERSION"
                                    )
                    );
        }

        @Test
        @DisplayName("이미 동의한 약관이 포함되면 409를 반환한다")
        void createConsent_alreadyAgreed()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            setAuthentication(userId);

            given(
                    consentCommandService.create(
                            any()
                    )
            ).willThrow(
                    new BusinessException(
                            ConsentErrorCode
                                    .CONSENT_ALREADY_AGREED
                    )
            );

            // when & then
            mockMvc.perform(
                            post(URI)
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentDocumentVersionIds": [
                                                "%s"
                                              ]
                                            }
                                            """.formatted(
                                                    versionId
                                            )
                                    )
                    )
                    .andExpect(
                            status().isConflict()
                    )
                    .andExpect(
                            jsonPath("$.success")
                                    .value(false)
                    )
                    .andExpect(
                            jsonPath(
                                    "$.error.errorCode"
                            )
                                    .value(
                                            "CONSENT_ALREADY_AGREED"
                                    )
                    );
        }
    }

    private void setAuthentication(
            UUID userId
    ) {
        UserContext userContext =
                UserContext.from(
                        userId.toString(),
                        "PATIENT"
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userContext,
                        null,
                        List.of()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        authentication
                );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("로그인 사용자 약관 동의 내역 조회")
    class FindMyConsents {

        @Test
        @DisplayName("사용자의 동의 및 철회 내역을 조회한다")
        void findMyConsents_success()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();

            UUID consentId1 = UUID.randomUUID();
            UUID consentDocumentId1 = UUID.randomUUID();
            UUID consentDocumentVersionId1 =
                    UUID.randomUUID();

            UUID consentId2 = UUID.randomUUID();
            UUID consentDocumentId2 = UUID.randomUUID();
            UUID consentDocumentVersionId2 =
                    UUID.randomUUID();

            LocalDateTime agreedAt1 =
                    LocalDateTime.of(
                            2026,
                            9,
                            1,
                            10,
                            30
                    );

            LocalDateTime agreedAt2 =
                    LocalDateTime.of(
                            2026,
                            8,
                            20,
                            14,
                            0
                    );

            LocalDateTime withdrawnAt2 =
                    LocalDateTime.of(
                            2026,
                            8,
                            25,
                            9,
                            30
                    );

            setAuthentication(userId);

            ConsentFindHistoryResult agreedConsent =
                    new ConsentFindHistoryResult(
                            consentId1,
                            consentDocumentId1,
                            consentDocumentVersionId1,
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            ConsentStatus.AGREED,
                            agreedAt1,
                            null
                    );

            ConsentFindHistoryResult withdrawnConsent =
                    new ConsentFindHistoryResult(
                            consentId2,
                            consentDocumentId2,
                            consentDocumentVersionId2,
                            "마케팅 정보 수신 동의",
                            "1.0",
                            ConsentStatus.WITHDRAWN,
                            agreedAt2,
                            withdrawnAt2
                    );

            given(
                    consentQueryService.findMyConsents(
                            userId
                    )
            ).willReturn(
                    List.of(
                            agreedConsent,
                            withdrawnConsent
                    )
            );

            // when & then
            mockMvc.perform(
                            get(
                                    "/api/v1/consents/me"
                            )
                    )
                    .andExpect(
                            status().isOk()
                    )
                    .andExpect(
                            jsonPath("$.success")
                                    .value(true)
                    )
                    .andExpect(
                            jsonPath("$.code")
                                    .value(200)
                    )
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "약관 동의 내역 조회 성공"
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content.length()"
                            )
                                    .value(2)
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[0].consentId"
                            )
                                    .value(
                                            consentId1.toString()
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[0].consentDocumentId"
                            )
                                    .value(
                                            consentDocumentId1.toString()
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[0].consentDocumentVersionId"
                            )
                                    .value(
                                            consentDocumentVersionId1
                                                    .toString()
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[0].title"
                            )
                                    .value(
                                            "개인정보 수집 및 이용 동의"
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[0].version"
                            )
                                    .value("1.0")
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[0].status"
                            )
                                    .value("AGREED")
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[0].withdrawnAt"
                            ).value(nullValue())
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[1].status"
                            )
                                    .value("WITHDRAWN")
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content[1].withdrawnAt"
                            )
                                    .value(
                                            withdrawnAt2.toString()
                                    )
                    );

            then(consentQueryService)
                    .should()
                    .findMyConsents(userId);
        }

        @Test
        @DisplayName("동의 내역이 없으면 빈 목록을 반환한다")
        void findMyConsents_empty()
                throws Exception {

            // given
            UUID userId = UUID.randomUUID();

            setAuthentication(userId);

            given(
                    consentQueryService.findMyConsents(
                            userId
                    )
            ).willReturn(List.of());

            // when & then
            mockMvc.perform(
                            get(
                                    "/api/v1/consents/me"
                            )
                    )
                    .andExpect(
                            status().isOk()
                    )
                    .andExpect(
                            jsonPath("$.success")
                                    .value(true)
                    )
                    .andExpect(
                            jsonPath("$.code")
                                    .value(200)
                    )
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "약관 동의 내역 조회 성공"
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content"
                            )
                                    .isArray()
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.content"
                            )
                                    .isEmpty()
                    );

            then(consentQueryService)
                    .should()
                    .findMyConsents(userId);
        }
    }
}