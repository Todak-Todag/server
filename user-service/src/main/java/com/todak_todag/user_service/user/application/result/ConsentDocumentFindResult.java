package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;

import java.util.UUID;

// 목록 조회 결과
public record ConsentDocumentFindResult(
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String consentType,
        String title,
        String version,
        boolean required
) {

    // Repository 조회 결과를 Application Result로 변환
    public static ConsentDocumentFindResult from(
            ConsentDocumentCurrentView view
    ) {
        return new ConsentDocumentFindResult(
                view.consentDocumentId(),
                view.consentDocumentVersionId(),
                view.consentType(),
                view.title(),
                view.version(),
                view.isRequired()
        );
    }
}