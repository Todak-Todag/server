package com.todak_todag.user_service.user.presentation.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.application.result.UserSearchResult;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

class UserSearchResponseTest {

	@Test
	@DisplayName("role은 UserRole이 아니라 한글명(getKoreaName)으로 매핑된다")
	void from_mapsRoleToKoreaName() {
		UUID userId = UUID.randomUUID();
		UUID regionId = UUID.randomUUID();
		UserSearchResult result = new UserSearchResult(
				userId,
				"김영수",
				"01012345678",
				"전라남도",
				"고흥군",
				regionId,
				UserStatus.APPROVED,
				UserRole.SOCIAL_WORKER,
				false
		);

		UserSearchResponse response = UserSearchResponse.from(result);

		assertThat(response.role()).isEqualTo("사회복지사");
		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.status()).isEqualTo(UserStatus.APPROVED);
	}
}
