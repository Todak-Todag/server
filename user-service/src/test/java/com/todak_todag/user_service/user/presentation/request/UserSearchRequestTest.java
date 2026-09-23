package com.todak_todag.user_service.user.presentation.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.application.query.UserSearchQuery;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

class UserSearchRequestTest {

	@Nested
	@DisplayName("role 코드 매핑")
	class ResolveRoles {

		@ParameterizedTest
		@CsvSource({
				"1, PATIENT",
				"2, HOSPITAL_STAFF",
				"3, SERVICE_PROVIDER",
				"4, SOCIAL_WORKER"
		})
		@DisplayName("1~4 코드는 단일 role로 매핑된다")
		void singleRole(int code, UserRole expected) {
			UserSearchRequest request = new UserSearchRequest(null, null, code, null);

			UserSearchQuery query = request.toQuery();

			assertThat(query.roles()).containsExactly(expected);
		}

		@Test
		@DisplayName("5 코드는 ADMIN과 MASTER를 함께 포함한다")
		void adminAndMaster() {
			UserSearchRequest request = new UserSearchRequest(null, null, 5, null);

			UserSearchQuery query = request.toQuery();

			assertThat(query.roles()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.MASTER);
		}

		@Test
		@DisplayName("6 코드는 전체 조회를 의미하며 roles는 null이다")
		void allRoles() {
			UserSearchRequest request = new UserSearchRequest(null, null, 6, null);

			UserSearchQuery query = request.toQuery();

			assertThat(query.roles()).isNull();
		}

		@ParameterizedTest
		@NullSource
		@ValueSource(ints = {0, 7, -1, 100})
		@DisplayName("role 코드가 없거나 정의되지 않은 값이면 기본값(전체)으로 처리되어 roles는 null이다")
		void defaultsToAllRoles(Integer code) {
			UserSearchRequest request = new UserSearchRequest(null, null, code, null);

			UserSearchQuery query = request.toQuery();

			assertThat(query.roles()).isNull();
		}
	}

	@Nested
	@DisplayName("status 코드 매핑")
	class ResolveStatus {

		@ParameterizedTest
		@CsvSource({
				"1, APPROVED",
				"2, SUSPENDED",
				"3, WITHDRAWN",
				"4, PENDING",
				"5, REJECTED"
		})
		@DisplayName("1~5 코드는 각 UserStatus로 매핑된다")
		void singleStatus(int code, UserStatus expected) {
			UserSearchRequest request = new UserSearchRequest(null, null, null, code);

			UserSearchQuery query = request.toQuery();

			assertThat(query.status()).isEqualTo(expected);
		}

		@Test
		@DisplayName("status 코드가 없으면 기본값인 APPROVED로 처리된다")
		void defaultsToApproved() {
			UserSearchRequest request = new UserSearchRequest(null, null, null, null);

			UserSearchQuery query = request.toQuery();

			assertThat(query.status()).isEqualTo(UserStatus.APPROVED);
		}

		@ParameterizedTest
		@ValueSource(ints = {0, 6, -1, 100})
		@DisplayName("정의되지 않은 status 코드는 기본값인 APPROVED로 처리된다")
		void undefinedCodeDefaultsToApproved(int code) {
			UserSearchRequest request = new UserSearchRequest(null, null, null, code);

			UserSearchQuery query = request.toQuery();

			assertThat(query.status()).isEqualTo(UserStatus.APPROVED);
		}
	}

	@Test
	@DisplayName("page/size는 변형 없이 Query에 그대로 전달된다")
	void pageAndSizePassThrough() {
		UserSearchRequest request = new UserSearchRequest(2, 30, 1, 1);

		UserSearchQuery query = request.toQuery();

		assertThat(query.page()).isEqualTo(2);
		assertThat(query.size()).isEqualTo(30);
	}

	@Test
	@DisplayName("role=전체, status 기본값 조합에서도 정상적으로 Query가 생성된다")
	void allRolesWithDefaultStatus() {
		UserSearchRequest request = new UserSearchRequest(null, null, 6, null);

		UserSearchQuery query = request.toQuery();

		assertThat(query.roles()).isNull();
		assertThat(query.status()).isEqualTo(UserStatus.APPROVED);
	}
}
