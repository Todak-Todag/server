package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.Consent;

import java.util.List;

public interface ConsentCommandRepository {

    // 동의 내역 저장
    List<Consent> saveAll(List<Consent> consents);
}