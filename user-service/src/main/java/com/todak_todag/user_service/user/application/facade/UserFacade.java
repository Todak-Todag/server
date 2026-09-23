package com.todak_todag.user_service.user.application.facade;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserAdminCreateCommand;
import com.todak_todag.user_service.user.application.command.UserDeleteCommand;
import com.todak_todag.user_service.user.application.command.UserPasswordUpdateCommand;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.application.service.command.UserUpdateService;
import com.todak_todag.user_service.user.application.service.query.UserCreateQueryService;
import com.todak_todag.user_service.user.domain.entity.Region;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserCreateQueryService userCreateQueryService;

    private final UserCreateService userCreateService;

    private final UserUpdateService userUpdateService;

    private final PasswordEncoderPort passwordEncoder;

    public UserSignupCreatedResult createUserSignup(UserSignupCommand signup) {
        Set<UUID> agreedIds = userCreateQueryService.validateSignup(signup);
        String passwordHash = passwordEncoder.encode(signup.password());

        return userCreateService.createUserSignup(signup, passwordHash, agreedIds);
    }

    public UserAdminCreatedResult createUserAdmin(UserAdminCreateCommand createAdmin) {
        Region region = userCreateQueryService.validateAdminCreate(createAdmin);
        String passwordHash = passwordEncoder.encode(createAdmin.password());

        return userCreateService.createUserAdmin(createAdmin, passwordHash, region);
    }

    public UserPatientCreatedResult createUserPatient(UserPatientCreateCommand createPatient) {
        userCreateQueryService.validatePatientCreate(createPatient);
        String passwordHash = passwordEncoder.encode(createPatient.password());

        return userCreateService.createUserPatient(createPatient, passwordHash);
    }

    public void userDelete(UserDeleteCommand command) {
        String currentPasswordHash = userUpdateService.findPasswordHashForVerification(command.requesterId());

        if (!passwordEncoder.matches(command.currentPassword(), currentPasswordHash)) {
            log.warn(
            		"[User] 현재 비밀번호와 일치하지 않은 회원탈퇴 요청이 들어왔습니다. userId={}",
            		command.requesterId()
            );
            
            throw new BusinessException(UserErrorCode.USER_INVALID_CURRENT_PASSWORD);
        }

        userUpdateService.userDelete(command);
    }

    public UUID passwordUpdate(UserPasswordUpdateCommand command) {
        String currentPasswordHash = userUpdateService.findPasswordHashForVerification(command.requesterId());

        if (!passwordEncoder.matches(command.currentPassword(), currentPasswordHash)) {
            log.warn(
            		"[User] 기존 비밀번호와 일치하지 않은 비밀번호 변경 요청이 들어왔습니다. userId={}",
            		command.requesterId()
            );
            
            throw new BusinessException(UserErrorCode.USER_INVALID_CURRENT_PASSWORD);
        }

        String newPasswordHash = passwordEncoder.encode(command.newPassword());

        return userUpdateService.passwordUpdate(command, newPasswordHash);
    }
}
