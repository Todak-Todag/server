package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.Consent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentCommandRepository {

    // 동의 내역 저장
    List<Consent> saveAll(List<Consent> consents);

    // 상태 변경 대상 동의 내역 조회
    Optional<Consent> findById(UUID consentId);
}