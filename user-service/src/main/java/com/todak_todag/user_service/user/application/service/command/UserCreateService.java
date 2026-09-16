package com.todak_todag.user_service.user.application.service.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.support.MaskingUtil;
import com.todak_todag.user_service.user.application.command.UserAdminCreateCommand;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.ConsentCommandRepository;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// User 생성 작업 담당 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreateService {

    private final UserCommandRepository userCommandRepo;
    private final UserQueryRepository userQueryRepo;
    private final ConsentCommandRepository consentCommandRepo;
    private final PasswordEncoderPort passwordEncoder; // createUserMaster용으로만 남는다

    @Transactional(rollbackFor = Exception.class)
    public UserSignupCreatedResult createUserSignup(UserSignupCommand signup, String passwordHash, Set<UUID> agreedIds) {
        // 저장 직전 재검증 — BCrypt 도는 동안 같은 아이디가 먼저 가입했을 수 있다
        assertUsernameStillAvailable(signup.username(), "회원가입");

        User signupUser = User.createSignup(
                signup.regionId(), signup.username(), passwordHash, signup.name(), signup.phone(), signup.type()
        );

        User user = saveOrThrowDuplicate(signupUser, signup.username(), "회원가입");

        LocalDateTime now = LocalDateTime.now();
        List<Consent> consents = agreedIds.stream()
                .map(verId -> Consent.agree(user.getId(), verId, now))
                .toList();
        consentCommandRepo.saveAll(consents);

        log.info("[User] 회원가입 완료 userId={}, role={}, regionId={}, agreedConsents={}",
                user.getId(), user.getRole(), user.getRegionId(), consents.size());

        return new UserSignupCreatedResult(user.getId(), user.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAdminCreatedResult createUserAdmin(UserAdminCreateCommand createAdmin, String passwordHash, Region region) {
        assertUsernameStillAvailable(createAdmin.username(), "운영자 등록");

        User admin = User.createAdmin(
                createAdmin.regionId(), createAdmin.username(), passwordHash, createAdmin.name(), createAdmin.phone()
        );

        User user = saveOrThrowDuplicate(admin, createAdmin.username(), "운영자 등록");

        log.info("[User] 운영자 계정 생성 완료 userId={}, regionId={}", user.getId(), user.getRegionId());

        return new UserAdminCreatedResult(user.getId(), user.getName(), region.getProvince(), region.getDistrict());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserPatientCreatedResult createUserPatient(UserPatientCreateCommand createPatient, String passwordHash) {
        assertUsernameStillAvailable(createPatient.username(), "퇴원 예정자 등록");

        User patient = User.createPatient(
                createPatient.regionId(), createPatient.username(), passwordHash,
                createPatient.name(), createPatient.phone(), createPatient.address()
        );

        User saved = saveOrThrowDuplicate(patient, createPatient.username(), "퇴원 예정자 등록");

        log.info("[User] 퇴원 예정자 등록 완료 userId={}, requesterId={}, regionId={}",
                saved.getId(), createPatient.requesterId(), saved.getRegionId());

        return new UserPatientCreatedResult(
                saved.getId(), createPatient.requesterId(), saved.getName(), saved.getPhone(), createPatient.regionId()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUserMaster(UUID userId, String username, String rawPassword, String name, String phone) {
        // 기동 시 1회 — 분리 대상 아님. 기존 로직 그대로
        if (userQueryRepo.initMasterDuplicate(userId)) return;

        String passwordHash = passwordEncoder.encode(rawPassword);
        User master = User.createMaster(userId, username, passwordHash, name, phone);
        userCommandRepo.save(master);

        log.info("[User] 마스터 계정 최초 생성 완료 userId={}", userId);
    }

    private void assertUsernameStillAvailable(String username, String action) {
        if (userQueryRepo.duplicateUsername(username)) {
            log.info("[User] 중복된 아이디로 {}이 시도되었습니다. username={}", action, MaskingUtil.maskUsername(username));
            throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
        }
    }

    // 재검증까지 통과한 뒤 그 찰나에 커밋된 동시 요청 — 유니크 인덱스(ux_p_users_username_active)가 최종 방어선
    private User saveOrThrowDuplicate(User user, String username, String action) {
        try {
            return userCommandRepo.save(user);
        } catch (DataIntegrityViolationException e) {
            log.info("[User] 동시 요청으로 {} 중 아이디 중복 충돌이 발생했습니다. username={}", action, MaskingUtil.maskUsername(username));
            throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
        }
    }
}