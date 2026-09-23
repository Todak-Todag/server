package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument;

public interface ConsentDocumentCommandRepository {

    // 약관 문서 등록
    ConsentDocument save(ConsentDocument consentDocument);
}