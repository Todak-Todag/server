package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentDocumentFindResult;

import java.util.List;

public record ConsentDocumentFindListResponse(
        List<ConsentDocumentFindResponse> content
) {

    public static ConsentDocumentFindListResponse of(
            List<ConsentDocumentFindResult> results
    ) {
        List<ConsentDocumentFindResponse> content = results.stream()
                .map(ConsentDocumentFindResponse::from)
                .toList();

        return new ConsentDocumentFindListResponse(content);
    }
}