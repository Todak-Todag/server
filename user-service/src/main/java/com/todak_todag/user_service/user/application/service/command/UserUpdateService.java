package com.todak_todag.user_service.user.application.service.command;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserDeleteCommand;
import com.todak_todag.user_service.user.application.command.UserPasswordUpdateCommand;
import com.todak_todag.user_service.user.application.port.TokenStorePort;
import com.todak_todag.user_service.user.application.support.AddressValidator;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final TokenStorePort tokenStorePort;
    private final AddressValidator addressValidator;
    private final UserQueryRepository userQueryRepo;
    private final AuthQueryRepository authQueryRepo;

    @Transactional(readOnly = true)
    public String findPasswordHashForVerification(UUID requesterId) {
        User user = userQueryRepo.findActiveById(requesterId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        return user.getPasswordHash();
    }

    @Transactional(rollbackFor = Exception.class)
    public void userDelete(UserDeleteCommand command) {
        // 재조회 — BCrypt 도는 동안 이미 탈퇴/변경됐다면 여기서 USER_NOT_FOUND로 걸러진다
        User user = userQueryRepo.findActiveById(command.requesterId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        user.delete(command.requesterId());

        Auth loginSession = authQueryRepo.findActiveByUserId(user.getId()).orElse(null);
        if (loginSession != null) {
            loginSession.logout();
        } else {
            log.warn("[User] 회원탈퇴 요청자의 현재 로그인 세션이 존재하지 않습니다. userId={}", user.getId());
        }

        tokenStorePort.revokeAllSessions(user.getId());
        log.info("[User] 회원탈퇴 완료 userId={}", user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public UUID passwordUpdate(UserPasswordUpdateCommand command, String newPasswordHash) {
        User user = userQueryRepo.findActiveById(command.requesterId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        user.changePassword(newPasswordHash);

        Auth loginSession = authQueryRepo.findActiveByUserId(user.getId()).orElse(null);
        if (loginSession != null) {
            loginSession.logout();
        } else {
            log.warn("[User] 비밀번호 변경 요청 사용자의 현재 로그인 세션이 존재하지 않습니다. userId={}", command.requesterId());
        }

        tokenStorePort.revokeAllSessions(user.getId());
        log.info("[User] 비밀번호 변경 완료 userId={}", user.getId());

        return user.getId();
    }
}