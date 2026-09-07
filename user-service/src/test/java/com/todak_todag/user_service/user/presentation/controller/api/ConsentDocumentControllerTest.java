package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentDocumentErrorCode;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.ConsentDocumentDeleteCommand;
import com.todak_todag.user_service.user.application.command.ConsentDocumentVersionCreateCommand;
import com.todak_todag.user_service.user.application.result.*;
import com.todak_todag.user_service.user.application.service.command.ConsentDocumentCommandService;
import com.todak_todag.user_service.user.application.service.query.ConsentDocumentQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import com.todak_todag.user_service.user.application.result.ConsentDocumentVersionCreateResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(ConsentDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsentDocumentControllerTest {

    private static final String URI = "/api/v1/consent-documents";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentDocumentQueryService consentDocumentQueryService;

    @MockitoBean
    private ConsentDocumentCommandService consentDocumentCommandService;

    @Nested
    @DisplayName("현재 적용 중인 약관 목록 조회")
    class FindCurrentConsentDocuments {

        @Test
        @DisplayName("현재 적용 중인 약관 목록 조회에 성공한다")
        void findCurrentConsentDocuments_success() throws Exception {
            // given
            UUID documentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            ConsentDocumentFindResult result =
                    new ConsentDocumentFindResult(
                            documentId,
                            versionId,
                            "TERMS_OF_SERVICE",
                            "서비스 이용약관",
                            "v2",
                            true
                    );

            given(consentDocumentQueryService.findCurrentConsentDocuments())
                    .willReturn(List.of(result));

            // when & then
            mockMvc.perform(get(URI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("현재 적용 중인 약관 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].consentDocumentId")
                            .value(documentId.toString()))
                    .andExpect(jsonPath("$.data.content[0].consentDocumentVersionId")
                            .value(versionId.toString()))
                    .andExpect(jsonPath("$.data.content[0].consentType")
                            .value("TERMS_OF_SERVICE"))
                    .andExpect(jsonPath("$.data.content[0].title")
                            .value("서비스 이용약관"))
                    .andExpect(jsonPath("$.data.content[0].version")
                            .value("v2"))
                    .andExpect(jsonPath("$.data.content[0].isRequired")
                            .value(true));
        }

        @Test
        @DisplayName("조회 가능한 약관이 없으면 빈 목록을 반환한다")
        void findCurrentConsentDocuments_empty() throws Exception {
            // given
            given(consentDocumentQueryService.findCurrentConsentDocuments())
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get(URI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("약관 상세 조회")
    class FindConsentDocumentDetail {

        @Test
        @DisplayName("약관 버전 상세 조회에 성공한다")
        void findConsentDocumentDetail_success() throws Exception {
            // given
            UUID documentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            LocalDateTime effectiveAt =
                    LocalDateTime.of(2026, 9, 1, 0, 0);

            ConsentDocumentFindDetailResult result =
                    new ConsentDocumentFindDetailResult(
                            documentId,
                            versionId,
                            "TERMS_OF_SERVICE",
                            "서비스 이용약관",
                            "v2",
                            "약관 본문입니다.",
                            true,
                            effectiveAt
                    );

            given(
                    consentDocumentQueryService.findConsentDocumentDetail(
                            versionId
                    )
            ).willReturn(result);

            // when & then
            mockMvc.perform(
                            get(URI + "/{consentDocumentVersionId}", versionId)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("약관 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.consentDocumentId")
                            .value(documentId.toString()))
                    .andExpect(jsonPath("$.data.consentDocumentVersionId")
                            .value(versionId.toString()))
                    .andExpect(jsonPath("$.data.consentType")
                            .value("TERMS_OF_SERVICE"))
                    .andExpect(jsonPath("$.data.title")
                            .value("서비스 이용약관"))
                    .andExpect(jsonPath("$.data.version")
                            .value("v2"))
                    .andExpect(jsonPath("$.data.content")
                            .value("약관 본문입니다."))
                    .andExpect(jsonPath("$.data.isRequired")
                            .value(true))
                    .andExpect(jsonPath("$.data.effectiveAt")
                            .value("2026-09-01T00:00:00"));
        }

        @Test
        @DisplayName("약관 버전 ID 형식이 올바르지 않으면 400을 반환한다")
        void findConsentDocumentDetail_invalidVersionId() throws Exception {
            mockMvc.perform(
                            get(
                                    URI + "/{consentDocumentVersionId}",
                                    "invalid-version-id"
                            )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("약관 필수 여부 변경")
    class UpdateConsentDocumentRequired {

        @Test
        @DisplayName("약관 필수 여부 변경에 성공한다")
        void updateConsentDocumentRequired_success() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();

            ConsentDocumentUpdateRequiredResult result =
                    new ConsentDocumentUpdateRequiredResult(
                            consentDocumentId,
                            false
                    );

            given(
                    consentDocumentCommandService.updateRequired(
                            any()
                    )
            ).willReturn(result);

            // when & then
            mockMvc.perform(
                            patch(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}/required",
                                    consentDocumentId
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "isRequired": false
                                            }
                                            """
                                    )
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success")
                            .value(true))
                    .andExpect(jsonPath("$.code")
                            .value(200))
                    .andExpect(jsonPath("$.message")
                            .value("약관 필수 여부 변경 성공"))
                    .andExpect(jsonPath("$.data.consentDocumentId")
                            .value(consentDocumentId.toString()))
                    .andExpect(jsonPath("$.data.isRequired")
                            .value(false));
        }
    }

    @Nested
    @DisplayName("약관 사용 종료")
    class DeleteConsentDocument {

        @Test
        @DisplayName("약관 논리 삭제에 성공한다")
        void deleteConsentDocument_success() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            setAuthentication(userId);

            // when & then
            mockMvc.perform(
                            delete(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}",
                                    consentDocumentId
                            )
                    )
                    .andExpect(status().isNoContent());

            then(consentDocumentCommandService)
                    .should()
                    .delete(
                            new ConsentDocumentDeleteCommand(
                                    consentDocumentId,
                                    userId
                            )
                    );
        }

        @Test
        @DisplayName("약관 문서 ID 형식이 올바르지 않으면 400을 반환한다")
        void deleteConsentDocument_invalidId() throws Exception {
            // given
            UUID userId = UUID.randomUUID();

            setAuthentication(userId);

            // when & then
            mockMvc.perform(
                            delete(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}",
                                    "invalid-id"
                            )
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 약관이면 404를 반환한다")
        void deleteConsentDocument_notFound() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            setAuthentication(userId);

            willThrow(
                    new BusinessException(
                            ConsentDocumentErrorCode
                                    .CONSENT_DOCUMENT_NOT_FOUND
                    )
            )
                    .given(consentDocumentCommandService)
                    .delete(any());

            // when & then
            mockMvc.perform(
                            delete(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}",
                                    consentDocumentId
                            )
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success")
                            .value(false))
                    .andExpect(jsonPath("$.error.errorCode")
                            .value("CONSENT_DOCUMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("이미 사용 종료된 약관이면 409를 반환한다")
        void deleteConsentDocument_alreadyDeleted() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            setAuthentication(userId);

            willThrow(
                    new BusinessException(
                            ConsentDocumentErrorCode
                                    .CONSENT_DOCUMENT_ALREADY_DELETED
                    )
            )
                    .given(consentDocumentCommandService)
                    .delete(any());

            // when & then
            mockMvc.perform(
                            delete(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}",
                                    consentDocumentId
                            )
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success")
                            .value(false))
                    .andExpect(jsonPath("$.error.errorCode")
                            .value("CONSENT_DOCUMENT_ALREADY_DELETED"));
        }
    }
    private void setAuthentication(UUID userId) {
        UserContext userContext =
                UserContext.from(
                        userId.toString(),
                        "MASTER"
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userContext,
                        null,
                        List.of()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("신규 약관 및 최초 버전 등록")
    class CreateConsentDocument {

        @Test
        @DisplayName("신규 약관과 최초 버전 등록에 성공한다")
        void createConsentDocument_success() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID consentDocumentVersionId = UUID.randomUUID();

            ConsentDocumentCreateResult result =
                    new ConsentDocumentCreateResult(
                            consentDocumentId,
                            consentDocumentVersionId
                    );

            given(
                    consentDocumentCommandService.create(
                            any()
                    )
            ).willReturn(result);

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/v1/admin/consent-documents"
                            )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentType": "PERSONAL_INFORMATION",
                                              "title": "개인정보 수집 및 이용 동의",
                                              "isRequired": true,
                                              "version": "1.0",
                                              "content": "개인정보 수집 및 이용 약관 내용입니다.",
                                              "effectiveAt": "2026-09-10T00:00:00"
                                            }
                                            """
                                    )
                    )
                    .andExpect(status().isCreated())
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
                                    .value("약관 등록 성공")
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.consentDocumentId"
                            )
                                    .value(
                                            consentDocumentId.toString()
                                    )
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.consentDocumentVersionId"
                            )
                                    .value(
                                            consentDocumentVersionId.toString()
                                    )
                    );
        }

        @Test
        @DisplayName("필수 입력값이 누락되면 400을 반환한다")
        void createConsentDocument_invalidRequest()
                throws Exception {

            mockMvc.perform(
                            post(
                                    "/api/v1/admin/consent-documents"
                            )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentType": "PERSONAL_INFORMATION",
                                              "title": "",
                                              "isRequired": true,
                                              "version": "1.0",
                                              "content": "약관 내용입니다."
                                            }
                                            """
                                    )
                    )
                    .andExpect(
                            status().isBadRequest()
                    );

            then(consentDocumentCommandService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("동일 유형의 약관이 이미 존재하면 409를 반환한다")
        void createConsentDocument_alreadyExists()
                throws Exception {

            // given
            given(
                    consentDocumentCommandService.create(
                            any()
                    )
            ).willThrow(
                    new BusinessException(
                            ConsentDocumentErrorCode
                                    .CONSENT_DOCUMENT_ALREADY_EXISTS
                    )
            );

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/v1/admin/consent-documents"
                            )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            """
                                            {
                                              "consentType": "PERSONAL_INFORMATION",
                                              "title": "개인정보 수집 및 이용 동의",
                                              "isRequired": true,
                                              "version": "1.0",
                                              "content": "개인정보 수집 및 이용 약관 내용입니다.",
                                              "effectiveAt": "2026-09-10T00:00:00"
                                            }
                                            """
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
                            jsonPath("$.error.errorCode")
                                    .value(
                                            "CONSENT_DOCUMENT_ALREADY_EXISTS"
                                    )
                    );
        }
    }

    @Nested
    @DisplayName("신규 약관 버전 등록 API")
    class CreateConsentDocumentVersion {

        @Test
        @DisplayName("신규 약관 버전을 등록하면 201을 반환한다")
        void createVersion_success() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID consentDocumentVersionId = UUID.randomUUID();

            ConsentDocumentVersionCreateResult result =
                    new ConsentDocumentVersionCreateResult(
                            consentDocumentVersionId
                    );

            given(
                    consentDocumentCommandService.createVersion(
                            any(ConsentDocumentVersionCreateCommand.class)
                    )
            ).willReturn(result);

            String request = """
                {
                    "version": "1.1",
                    "content": "변경된 약관 내용",
                    "effectiveAt": "2026-09-10T00:00:00"
                }
                """;

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}/versions",
                                    consentDocumentId
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(request)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("약관 버전 등록 성공")
                    )
                    .andExpect(
                            jsonPath(
                                    "$.data.consentDocumentVersionId"
                            ).value(
                                    consentDocumentVersionId.toString()
                            )
                    );

            verify(consentDocumentCommandService)
                    .createVersion(
                            any(ConsentDocumentVersionCreateCommand.class)
                    );
        }

        @Test
        @DisplayName("필수 값이 누락되면 400을 반환한다")
        void createVersion_invalidRequest() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();

            String request = """
                {
                    "version": "",
                    "content": "",
                    "effectiveAt": null
                }
                """;

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}/versions",
                                    consentDocumentId
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(request)
                    )
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(
                    consentDocumentCommandService
            );
        }

        @Test
        @DisplayName("존재하지 않는 약관 문서이면 404를 반환한다")
        void createVersion_documentNotFound() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();

            given(
                    consentDocumentCommandService.createVersion(
                            any(ConsentDocumentVersionCreateCommand.class)
                    )
            ).willThrow(
                    new BusinessException(
                            ConsentDocumentErrorCode
                                    .CONSENT_DOCUMENT_NOT_FOUND
                    )
            );

            String request = """
                {
                    "version": "1.1",
                    "content": "변경된 약관 내용",
                    "effectiveAt": "2026-09-10T00:00:00"
                }
                """;

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}/versions",
                                    consentDocumentId
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(request)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.error.errorCode")
                                    .value(
                                            ConsentDocumentErrorCode
                                                    .CONSENT_DOCUMENT_NOT_FOUND
                                                    .getCode()
                                    )
                    );
        }

        @Test
        @DisplayName("동일한 버전이 존재하면 409를 반환한다")
        void createVersion_duplicateVersion() throws Exception {
            // given
            UUID consentDocumentId = UUID.randomUUID();

            given(
                    consentDocumentCommandService.createVersion(
                            any(ConsentDocumentVersionCreateCommand.class)
                    )
            ).willThrow(
                    new BusinessException(
                            ConsentDocumentErrorCode
                                    .CONSENT_DOCUMENT_VERSION_ALREADY_EXISTS
                    )
            );

            String request = """
                {
                    "version": "1.1",
                    "content": "변경된 약관 내용",
                    "effectiveAt": "2026-09-10T00:00:00"
                }
                """;

            // when & then
            mockMvc.perform(
                            post(
                                    "/api/v1/admin/consent-documents/{consentDocumentId}/versions",
                                    consentDocumentId
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(request)
                    )
                    .andExpect(status().isConflict())
                    .andExpect(
                            jsonPath("$.error.errorCode")
                                    .value(
                                            ConsentDocumentErrorCode
                                                    .CONSENT_DOCUMENT_VERSION_ALREADY_EXISTS
                                                    .getCode()
                                    )
                    );
        }
    }
}