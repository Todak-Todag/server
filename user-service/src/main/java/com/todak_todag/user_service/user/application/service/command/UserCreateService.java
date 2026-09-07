package com.todak_todag.user_service.user.application.service.command;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentErrorCode;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserAdminCreateCommand;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.support.AddressValidator;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

// User 생성 작업 담당 서비스
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserCreateService {
	
	private final AddressValidator addressValidator;
	
	private final PasswordEncoderPort passwordEncoder;
	
	private final UserCommandRepository userCommandRepo;
	
	private final UserQueryRepository userQueryRepo;
	
	private final RegionQueryRepository regionQueryRepo;
	
	private final ConsentDocumentQueryRepository consentDocumentQueryRepo;
	
	public UserSignupCreatedResult createUserSignup(UserSignupCommand signup) {
		
		// 요청에 지역ID 존재하면 regionId 검증
		if(signup.regionId() != null) {
			if(!regionQueryRepo.existsAvailableRegion(signup.regionId())) {
				throw new BusinessException(RegionErrorCode.REGION_NOT_FOUND);
			}
		}
		
		// Username 중복 검증 : 가벼운 작업 위로
		if(userQueryRepo.duplicateUsername(signup.username())) {
			throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
		}
		
		// 현재 적용 중인 전체약관 조회
		Set<UUID> requestTermsIds = new HashSet<>(signup.getTermsIds());
		if(requestTermsIds.size() != signup.getTermsIds().size()) {
			throw new BusinessException(ConsentErrorCode.DUPLICATE_CONSENT_DOCUMENT_VERSION);
		}
				
		// 연산 빠르게 Map<UUID, ConsentDocumentCurrentView> 매핑
		Map<UUID, ConsentDocumentCurrentView> currentDocumentVersion = consentDocumentQueryRepo.findAllCurrent(LocalDateTime.now())
				.stream()
				.collect(Collectors.toMap(ConsentDocumentCurrentView::consentDocumentVersionId, version -> version));
		
		// agreements.agreed 가 true 로 넘어온 termsId 를 추출
		Set<UUID> agreedVersionIds = signup.agreements().stream()
				.filter(UserSignupCommand.AgreementCommand::agreed)
				.map(UserSignupCommand.AgreementCommand::termsId)
				.collect(Collectors.toSet());
		
		if(!currentDocumentVersion.keySet().containsAll(agreedVersionIds)) {
			throw new BusinessException(ConsentErrorCode.INVALID_CONSENT_DOCUMENT_VERSION);
		}
		
		Set<UUID> requiredVersionIds = currentDocumentVersion.values().stream()
				.filter(ConsentDocumentCurrentView::isRequired)
				.map(ConsentDocumentCurrentView::consentDocumentVersionId)
				.collect(Collectors.toSet());
		
		if(!agreedVersionIds.containsAll(requiredVersionIds)) {
			throw new BusinessException(UserErrorCode.USER_SIGNUP_REQUIRED_NOT_AGREED);
		}
		
		// 비밀번호 해시
		String passwordHash = passwordEncoder.encode(signup.password());
		
		// 회원가입용 User 팩토리 생성자
		User signupUser = User.createSignup(
				signup.regionId(),
				signup.username(),
				passwordHash,
				signup.name(),
				signup.phone(),
				signup.type()
		);
		
		User user = userCommandRepo.save(signupUser);
		// List<Consent> saveAll
		
		return new UserSignupCreatedResult(user.getId(), user.getName());
	}
	
	public UserAdminCreatedResult createUserAdmin(UserAdminCreateCommand createAdmin) {
		Region region = regionQueryRepo.findById(createAdmin.regionId())
        .orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
		
		// Username 중복 검증
		if(userQueryRepo.duplicateUsername(createAdmin.username())) {
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
		
		return new UserAdminCreatedResult(user.getId(), user.getName(), region.getProvince(), region.getDistrict());
	}
	
	public UserPatientCreatedResult createUserPatient(UserPatientCreateCommand createPatient) {
		// 1. 지역 정보 검증
		addressValidator.patientAddressValidate(createPatient);
		
		// 2. 중복 username 검증
		if(userQueryRepo.duplicateUsername(createPatient.username())) {
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
	}
	
}
