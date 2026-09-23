package com.todak_todag.user_service.user.application.service.query;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.support.MaskingUtil;
import com.todak_todag.user_service.user.application.command.UserAdminCreateCommand;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.support.AddressValidator;
import com.todak_todag.user_service.user.application.support.ConsentDocumentValidator;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCreateQueryService {

	private final ConsentDocumentValidator consentDocumentValidator;
  
	private final AddressValidator addressValidator;
  
	private final UserQueryRepository userQueryRepo;
  
	private final RegionQueryRepository regionQueryRepo;
	
	public Set<UUID> validateSignup(UserSignupCommand signup) {
		if(signup.regionId() != null && !regionQueryRepo.existsAvailableRegion(signup.regionId())) {
			log.info(
					"[User] 존재하지 않는 지역으로 회원가입이 시도되었습니다. regionId={}",
					signup.regionId()
			);
			
			throw new BusinessException(RegionErrorCode.REGION_NOT_FOUND);
		}
		
		if(userQueryRepo.duplicateUsername(signup.username())) {
			log.info(
					"[User] 중복된 아이디로 회원가입이 시도되었습니다. username={}",
					MaskingUtil.maskUsername(signup.username())
			);
			
			throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
		}
		
		return consentDocumentValidator.signupConsentDocumentValidate(signup);
	}
	
	public Region validateAdminCreate(UserAdminCreateCommand createAdmin) {
		Region region = regionQueryRepo.findById(createAdmin.regionId())
        .orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
		
		if(!region.isActive()) {
			throw new BusinessException(CommonErrorCode.REGION_NOT_SUPPORTED);
		}
		
		if (userQueryRepo.duplicateUsername(createAdmin.username())) {
      log.info(
      		"[User] 중복된 아이디로 운영자 등록이 시도되었습니다. username={}",
      		MaskingUtil.maskUsername(createAdmin.username())
      );
      
      throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
		}
		
		return region;
	}
	
	public void validatePatientCreate(UserPatientCreateCommand createPatient) {
		addressValidator.patientAddressValidate(createPatient);
		
		if(userQueryRepo.duplicateUsername(createPatient.username())) {
			log.info(
					"[User] 중복된 아이디로 퇴원 예정자 등록이 시도되었습니다. username={}",
					MaskingUtil.maskUsername(createPatient.username())
			);
			
			throw new BusinessException(UserErrorCode.USER_DUPLICATE_LOGIN_ID);
		}
	}
}
