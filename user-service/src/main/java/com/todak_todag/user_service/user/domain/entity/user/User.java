package com.todak_todag.user_service.user.domain.entity.user;

import java.util.UUID;

import com.todak_todag.user_service.global.common.BaseAuditableEntity;
import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseAuditableEntity {

	@Id
	@Column(name = "user_id")
	private UUID id;

	@Column(name = "region_id")
	private UUID regionId;

	@Column(name = "username", unique = true, nullable = false, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "phone", nullable = false, length = 20)
	private String phone;

	@Column(name = "status_change_reason")
	private String statusChangeReason;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status;
	
	@Column(name = "address")
	private String address;

	public static User createSignup(
			UUID regionId,
			String username,
			String passwordHash,
			String name,
			String phone,
			UserRole role
	) {
		validateSignup(role);
		
		User user = new User();
		
		user.id = UUID.randomUUID();
		user.regionId = regionId;
		user.username = username;
		user.passwordHash = passwordHash;
		user.name = name;
		user.phone = phone;
		user.role = role;
		user.status = UserStatus.PENDING;
		
		return user;
	}
	
	public static User createAdmin(
			UUID regionId,
			String username,
			String passwordHash,
			String name,
			String phone
	) {
		
		User user = new User();
		
		user.id = UUID.randomUUID();
		user.regionId = regionId;
		user.username = username;
		user.passwordHash = passwordHash;
		user.name = name;
		user.phone = phone;
		user.role = UserRole.ADMIN;
		user.status = UserStatus.APPROVED;
		
		return user;
	}
	
	public static User createPatient(
			UUID regionId,
			String username,
			String passwordHash,
			String name,
			String phone,
			String address
	) {
		
		User user = new User();
		
		user.id = UUID.randomUUID();
		user.regionId = regionId;
		user.username = username;
		user.passwordHash = passwordHash;
		user.name = name;
		user.phone = phone;
		user.role = UserRole.PATIENT;
		user.address = address;
		user.status = UserStatus.WITHDRAWN;
		
		return user;
	}
	
	public static User createMaster(
			UUID userId,
			String username,
			String passwordHash,
			String name,
			String phone
	) {
		User user = new User();

		user.id = userId;
		user.regionId = null;
		user.username = username;
		user.passwordHash = passwordHash;
		user.name = name;
		user.phone = phone;
		user.role = UserRole.MASTER;
		user.status = UserStatus.APPROVED;

		return user;
	}

	private static void validateSignup(UserRole role) {
		// 회원가입 Role 검증
		switch (role) {
			case HOSPITAL_STAFF, SERVICE_PROVIDER, SOCIAL_WORKER -> {}
			default -> { throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_ROLE); }
		}
	}
	
	public void delete(UUID deletedBy) {
		this.markDeleted(deletedBy);
		this.username = UUID.randomUUID().toString();
		this.name = "DELETE";
		this.phone = "01000000000";
		this.regionId = null;
		this.address = null;
		this.passwordHash = "DELETE";
	}
	
	public void changePassword(String passwordHash) {
		validateApproved();
		
		this.passwordHash = passwordHash;
	}
	
	public void changeMyInfo(String name, String phone, UUID regionId, String address) {
		validateApproved();
		
		if(!(name == null || name.isBlank())) {
			changeName(name);
		}
		
		if(!(phone == null || phone.isBlank())) {
			changePhone(phone);
		}
		
		if(regionId != null) {
			changeRegion(regionId);
		}
		
		if(!(address == null || address.isBlank())) {
			changeAddress(address);
		}
	}
	
	private void changeName(String name) {
		this.name = name;
	}
	
	private void changePhone(String phone) {
		this.phone = phone;
	}
	
	private void changeAddress(String address) {
		this.address = address;
	}
	
	private void changeRegion(UUID regionId) {
		this.regionId = regionId;
	}
	
	public void validateCanLogin() {
		switch (this.status) {
			case APPROVED, WITHDRAWN -> {}
			
			case PENDING -> { throw new BusinessException(UserErrorCode.USER_NOT_APPROVAL); }
			
			case SUSPENDED -> { throw new BusinessException(UserErrorCode.USER_SUSPENDED); }
			
			default -> { throw new BusinessException(CommonErrorCode.SERVICE_ACCESS_DENIED); }
		}
	}
	
	public void validateApproved() {
		switch (this.status) {
			case APPROVED -> {}
			default -> { throw new BusinessException(CommonErrorCode.SERVICE_ACCESS_DENIED); }
		}
	}
	
	public boolean isWithdrawn() {
		return this.status == UserStatus.WITHDRAWN;
	}
	
	public boolean isPending() {
		return this.status == UserStatus.PENDING;
	}
	
	public boolean isRegion() {
		return this.regionId != null;
	}
	
	public boolean isApprove() {
		return this.status == UserStatus.APPROVED;
	}
	
	public void suspend(String statusChangeReason) {
		this.statusChangeReason = statusChangeReason;
		if(this.isApprove()) {
			this.status = UserStatus.SUSPENDED;
		}
	}
	
	public boolean isPatient() {
		return this.role == UserRole.PATIENT;
	}
	
	public void approvalOrReject(Boolean accept, String rejectReason) {
		// 승인
		if(accept == true) {
			if(rejectReason != null) {
				throw new BusinessException(UserErrorCode.USER_APPROVAL_CONFLICT);
			}
			
			if(isPending()) {
				this.status = UserStatus.APPROVED;
				this.statusChangeReason = null;
				return;
			}
			
			throw new BusinessException(UserErrorCode.USER_MODIFY_STATE);
		}
		
		// 거절
		if(rejectReason == null || rejectReason.isBlank()) {
			throw new BusinessException(UserErrorCode.USER_REJECT_CONFLICT);
		}
		
		if(isPending()) {
			this.status = UserStatus.REJECTED;
			this.statusChangeReason = rejectReason;
			return;
		}
		
		throw new BusinessException(UserErrorCode.USER_MODIFY_STATE);
	}

	// 생성된 아이디를 통해 환자가 로그인 후
	// 필수 약관 동의 시 승인 상태로 변경.
	public void approveFromRequiredConsent() {
		if (isPatient() && isWithdrawn()) {
			this.status = UserStatus.APPROVED;
			this.statusChangeReason = null;
		}
	}

	// Approved인 상태의 사용자만 WITHDRAWN으로 변경 가능
	public void withdrawFromRequiredConsent() {
		if (isApprove()) {
			this.status = UserStatus.WITHDRAWN;
		}
	}
}
