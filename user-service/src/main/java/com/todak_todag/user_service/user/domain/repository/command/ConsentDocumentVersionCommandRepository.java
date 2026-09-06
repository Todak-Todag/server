package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;

public interface ConsentDocumentVersionCommandRepository {

    // 약관 버전 등록
    ConsentDocumentVersion save(
            ConsentDocumentVersion consentDocumentVersion
    );
}