package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.user.application.result.ConsentDocumentFindResult;
import com.todak_todag.user_service.user.application.service.query.ConsentDocumentQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsentDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsentDocumentControllerTest {

    private static final String URI = "/api/v1/consent-documents";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentDocumentQueryService consentDocumentQueryService;

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
}