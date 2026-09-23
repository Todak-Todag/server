package com.todak_todag.user_service.user.domain.repository.query;

import java.util.UUID;

/**
 * infrastructure에서 application의 Result를 직접 참조하게 하면 계층 방향이 이상해져서
 * Domain Repository용 View.
 */
public record ConsentDocumentCurrentView(
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String consentType,
        String title,
        String version,
        boolean isRequired
) {
}