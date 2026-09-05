package com.todak_todag.user_service.user.application.service.query;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.result.UserInternalReadResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserQueryRepository userQueryRepository;

    @InjectMocks
    private UserQueryService userQueryService;

    @Nested
    @DisplayName("내부 API 사용자 조회")
    class GetUser {

        @Test
        @DisplayName("활성 사용자를 조회하면 조회 결과를 반환한다")
        void getUser_success() {
            UUID userId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User user = Mockito.mock(User.class);

            given(user.getId()).willReturn(userId);
            given(user.getRole()).willReturn(UserRole.PATIENT);
            given(user.getRegionId()).willReturn(regionId);
            given(userQueryRepository.findActiveById(userId))
                    .willReturn(Optional.of(user));

            UserInternalReadResult result = userQueryService.getUser(userId);

            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.role()).isEqualTo(UserRole.PATIENT);
            assertThat(result.regionId()).isEqualTo(regionId);

            then(userQueryRepository).should().findActiveById(userId);
        }

        @Test
        @DisplayName("regionId가 없는 사용자를 조회하면 regionId가 null인 결과를 반환한다")
        void getUser_nullRegionId() {
            UUID userId = UUID.randomUUID();
            User user = Mockito.mock(User.class);

            given(user.getId()).willReturn(userId);
            given(user.getRole()).willReturn(UserRole.MASTER);
            given(user.getRegionId()).willReturn(null);
            given(userQueryRepository.findActiveById(userId))
                    .willReturn(Optional.of(user));

            UserInternalReadResult result = userQueryService.getUser(userId);

            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.role()).isEqualTo(UserRole.MASTER);
            assertThat(result.regionId()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 조회하면 예외가 발생한다")
        void getUser_notFound() {
            UUID userId = UUID.randomUUID();

            given(userQueryRepository.findActiveById(userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userQueryService.getUser(userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }
}
