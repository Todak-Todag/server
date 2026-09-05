package com.todak_todag.user_service.user.application.service.query;

import java.util.Optional;
import java.util.Set;
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
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.result.UserInternalReadResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserQueryRepository userQueryRepository;

    @Mock
    private RegionQueryRepository regionQueryRepository;

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

    @Nested
    @DisplayName("매칭 가능한 사회복지사 조회")
    class GetMatchableSocialWorkers {

        @Test
        @DisplayName("지역이 유효하면 매칭 가능한 사회복지사 식별자 목록을 반환한다")
        void getMatchableSocialWorkers_success() {
            UUID patientId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            UUID socialWorkerId = UUID.randomUUID();
            User patient = Mockito.mock(User.class);

            given(patient.isPatient()).willReturn(true);
            given(patient.getRegionId()).willReturn(regionId);
            given(patient.getAddress()).willReturn("전라남도 고흥군 도양읍");
            given(userQueryRepository.findById(patientId))
                    .willReturn(Optional.of(patient));
            given(regionQueryRepository.existsAvailableRegion(regionId))
                    .willReturn(true);
            given(userQueryRepository.findMatchableSocialWorkerIds(regionId))
                    .willReturn(Set.of(socialWorkerId));

            Set<UUID> result = userQueryService.getMatchableSocialWorkers(patientId);

            assertThat(result).containsExactly(socialWorkerId);

            then(userQueryRepository).should().findMatchableSocialWorkerIds(regionId);
        }

        @Test
        @DisplayName("존재하지 않는 환자를 조회하면 예외가 발생하고 이후 검증을 수행하지 않는다")
        void getMatchableSocialWorkers_patientNotFound() {
            UUID patientId = UUID.randomUUID();

            given(userQueryRepository.findById(patientId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userQueryService.getMatchableSocialWorkers(patientId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            then(regionQueryRepository).should(never()).existsAvailableRegion(any(UUID.class));
            then(userQueryRepository).should(never()).findMatchableSocialWorkerIds(any(UUID.class));
        }

        @Test
        @DisplayName("대상이 퇴원 예정자(PATIENT)가 아니면 USER_APPROVAL_CONFLICT 예외가 발생하고 이후 검증을 수행하지 않는다")
        void getMatchableSocialWorkers_notPatient() {
            UUID patientId = UUID.randomUUID();
            User patient = Mockito.mock(User.class);

            given(patient.isPatient()).willReturn(false);
            given(userQueryRepository.findById(patientId))
                    .willReturn(Optional.of(patient));

            assertThatThrownBy(() -> userQueryService.getMatchableSocialWorkers(patientId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_APPROVAL_CONFLICT);

            then(regionQueryRepository).should(never()).existsAvailableRegion(any(UUID.class));
            then(userQueryRepository).should(never()).findMatchableSocialWorkerIds(any(UUID.class));
        }

        @Test
        @DisplayName("환자의 지역 ID가 없으면 USER_PATIENT_INVALID_REGION 예외가 발생한다")
        void getMatchableSocialWorkers_noRegionId() {
            UUID patientId = UUID.randomUUID();
            User patient = Mockito.mock(User.class);

            given(patient.isPatient()).willReturn(true);
            given(patient.getRegionId()).willReturn(null);
            given(userQueryRepository.findById(patientId))
                    .willReturn(Optional.of(patient));

            assertThatThrownBy(() -> userQueryService.getMatchableSocialWorkers(patientId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_PATIENT_INVALID_REGION);
        }

        @Test
        @DisplayName("환자의 주소가 없으면 USER_PATIENT_INVALID_REGION 예외가 발생한다")
        void getMatchableSocialWorkers_noAddress() {
            UUID patientId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User patient = Mockito.mock(User.class);

            given(patient.isPatient()).willReturn(true);
            given(patient.getRegionId()).willReturn(regionId);
            given(patient.getAddress()).willReturn(null);
            given(userQueryRepository.findById(patientId))
                    .willReturn(Optional.of(patient));

            assertThatThrownBy(() -> userQueryService.getMatchableSocialWorkers(patientId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_PATIENT_INVALID_REGION);
        }

        @Test
        @DisplayName("서비스 지원 지역이 아니면 REGION_NOT_SUPPORTED 예외가 발생하고 매칭 조회를 수행하지 않는다")
        void getMatchableSocialWorkers_regionNotSupported() {
            UUID patientId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User patient = Mockito.mock(User.class);

            given(patient.isPatient()).willReturn(true);
            given(patient.getRegionId()).willReturn(regionId);
            given(patient.getAddress()).willReturn("전라남도 고흥군 도양읍");
            given(userQueryRepository.findById(patientId))
                    .willReturn(Optional.of(patient));
            given(regionQueryRepository.existsAvailableRegion(regionId))
                    .willReturn(false);

            assertThatThrownBy(() -> userQueryService.getMatchableSocialWorkers(patientId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(CommonErrorCode.REGION_NOT_SUPPORTED);

            then(userQueryRepository).should(never()).findMatchableSocialWorkerIds(any(UUID.class));
        }

        @Test
        @DisplayName("매칭되는 사회복지사가 없으면 빈 목록을 반환한다")
        void getMatchableSocialWorkers_empty() {
            UUID patientId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User patient = Mockito.mock(User.class);

            given(patient.isPatient()).willReturn(true);
            given(patient.getRegionId()).willReturn(regionId);
            given(patient.getAddress()).willReturn("전라남도 고흥군 도양읍");
            given(userQueryRepository.findById(patientId))
                    .willReturn(Optional.of(patient));
            given(regionQueryRepository.existsAvailableRegion(regionId))
                    .willReturn(true);
            given(userQueryRepository.findMatchableSocialWorkerIds(regionId))
                    .willReturn(Set.of());

            Set<UUID> result = userQueryService.getMatchableSocialWorkers(patientId);

            assertThat(result).isEmpty();
        }
    }
}
