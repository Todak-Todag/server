package com.todak_todag.user_service.user.application.service.command;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.support.MaskingUtil;
import com.todak_todag.user_service.user.application.command.UserAdminCreateCommand;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.support.ConsentDocumentValidator;
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
	
	private final ConsentDocumentValidator consentDocumentValidator;
	
	private final PasswordEncoderPort passwordEncoder;
	
	private final UserCommandRepository userCommandRepo;
	
	private final UserQueryRepository userQueryRepo;
	
	private final ConsentCommandRepository consentCommandRepo;
	
	public UserSignupCreatedResult createUserSignup(UserSignupCommand signup, String passwordHsah, Set<UUID> agreedIds) {
		
	}
	
	public UserAdminCreatedResult createUserAdmin(UserAdminCreateCommand createAdmin) {
		Region region = regionQueryRepo.findById(createAdmin.regionId())
        .orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
		
		// Username 중복 검증
		if(userQueryRepo.duplicateUsername(createAdmin.username())) {
			log.info(
					"[User] 중복된 아이디로 운영자 등록이 시도되었습니다. username={}",
					MaskingUtil.maskUsername(createAdmin.username())
			);

			throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
		}
		
		// 비밀번호 해시
		String passwordHash = passwordEncoder.encode(createAdmin.password());
		
		User admin = User.createAdmin(
				createAdmin.regionId(),
				createAdmin.username(),
				passwordHash,
				createAdmin.name(),
				createAdmin.phone()
		);
		
		User user = userCommandRepo.save(admin);

		// 권한이 높은 계정의 생성은 감사 대상이라 성공도 남긴다.
		log.info(
				"[User] 운영자 계정 생성 완료 userId={}, regionId={}",
				user.getId(),
				user.getRegionId()
		);

		return new UserAdminCreatedResult(user.getId(), user.getName(), region.getProvince(), region.getDistrict());
	}
	
	public UserPatientCreatedResult createUserPatient(UserPatientCreateCommand createPatient) {
		// 1. 지역 정보 검증
		addressValidator.patientAddressValidate(createPatient);
		
		// 2. 중복 username 검증
		if(userQueryRepo.duplicateUsername(createPatient.username())) {
			log.info(
					"[User] 중복된 아이디로 퇴원 예정자 등록이 시도되었습니다. username={}",
					MaskingUtil.maskUsername(createPatient.username())
			);

			throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
		}
		
		// 3. passwordHash
		String passwordHash = passwordEncoder.encode(createPatient.password());
		
		// 4. 퇴원 예정자 생성
		User patient = User.createPatient(
				createPatient.regionId(),
				createPatient.username(),
				passwordHash,
				createPatient.name(),
				createPatient.phone(),
				createPatient.address()
		);
		
		// 5. 저장
		User saved = userCommandRepo.save(patient);

		log.info(
				"[User] 퇴원 예정자 등록 완료 userId={}, requesterId={}, regionId={}",
				saved.getId(),
				createPatient.requesterId(),
				saved.getRegionId()
		);

		return new UserPatientCreatedResult(
				saved.getId(),
				createPatient.requesterId(),
				saved.getName(),
				saved.getPhone(),
				createPatient.regionId()
		);
	}

	// 서버 최초 구동 시 마스터 계정이 없으면 생성한다 (있으면 아무 것도 하지 않음)
	public void createUserMaster(UUID userId, String username, String rawPassword, String name, String phone) {
		if(userQueryRepo.initMasterDuplicate(userId)) {
			return;
		}

		String passwordHash = passwordEncoder.encode(rawPassword);

		User master = User.createMaster(userId, username, passwordHash, name, phone);

		userCommandRepo.save(master);

		log.info("[User] 마스터 계정 최초 생성 완료 userId={}", userId);
	}
	
}
