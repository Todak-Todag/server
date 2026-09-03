package com.todak_todag.user_service.user.domain.entity.user;

import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

import com.todak_todag.user_service.global.common.BaseAuditableEntity;
import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
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
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_id")
	private UUID id;

	@Column(name = "region_id", nullable = false)
	private UUID regionId;

	@Column(name = "username", unique = true, nullable = false, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "phone", nullable = false, length = 20)
	private String phone;

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
		
		user.regionId = regionId;
		user.username = username;
		user.passwordHash = passwordHash;
		user.name = name;
		user.phone = phone;
		user.role = role;
		user.status = UserStatus.PENDING;
		
		return user;
	}
	
	public static User createAdmin() {
		// TODO: 운영자 등록 API
		return null;
	}
	
	public static User createPatient() {
		// TODO: 퇴원 예정자 등록 API
		return null;
	}
	
	private static void validateSignup(UserRole role) {
		// 회원가입 Role 검증
		switch (role) {
			case HOSPITAL_STAFF, SERVICE_PROVIDER, SOCIAL_WORKER -> {}
			default -> { throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_ROLE); }
		}
	}
	
	private static void validateAdmin(UserRole role) {
		/// 운영자 등록 Role 검증
		switch (role) {
			case ADMIN -> {}
			default -> { throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_ROLE); }
		}
	}
	
	private static void validatePatient(UserRole role) {
		// 퇴원 예정자 등록 Role 검증
		switch (role) {
			case PATIENT -> {}
			default -> { throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_ROLE); }
		}
	}
	
	public void changeName(String name) {
		this.name = name;
	}
	
	public void changePhone(String phone) {
		this.phone = phone;
	}
	
	public void changeAddress(String address) {
		this.address = address;
	}
	
	public void changeRegion(UUID regionId) {
		this.regionId = regionId;
	}
}
