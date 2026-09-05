package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentDetailView;

import java.time.LocalDateTime;
import java.util.UUID;

// 약관 버전 상세 내용
public record ConsentDocumentFindDetailResult(
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String consentType,
        String title,
        String version,
        String content,
        boolean required,
        LocalDateTime effectiveAt
) {

    public static ConsentDocumentFindDetailResult from(
            ConsentDocumentDetailView view
    ) {
        return new ConsentDocumentFindDetailResult(
                view.consentDocumentId(),
                view.consentDocumentVersionId(),
                view.consentType(),
                view.title(),
                view.version(),
                view.content(),
                view.isRequired(),
                view.effectiveAt()
        );
    }
}