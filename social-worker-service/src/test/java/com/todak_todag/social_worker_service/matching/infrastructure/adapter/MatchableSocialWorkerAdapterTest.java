package com.todak_todag.social_worker_service.matching.infrastructure.adapter;

import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserMatchableSocialWorkersResponse;
import com.todak_todag.social_worker_service.matching.infrastructure.client.UserServiceClient;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MatchableSocialWorkerAdapterTest {

    private UserServiceClient userServiceClient;
    private MatchableSocialWorkerAdapter adapter;

    @BeforeEach
    void setUp() {

        userServiceClient =
                mock(UserServiceClient.class);

        adapter =
                new MatchableSocialWorkerAdapter(
                        userServiceClient
                );
    }

    @Test
    @DisplayName("사회복지사 후보가 존재하면 후보 ID 목록을 반환한다")
    void returnsMatchableSocialWorkerIds() {

        UUID patientId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenReturn(
                ApiResponse.ok(
                        "사회복지사 정보 조회 성공",
                        new UserMatchableSocialWorkersResponse(
                                Set.of(workerId)
                        )
                )
        );

        Set<UUID> result =
                adapter.findMatchableSocialWorkerIds(
                        patientId
                );

        assertEquals(
                Set.of(workerId),
                result
        );

        verify(
                userServiceClient,
                times(1)
        ).getMatchableSocialWorkers(patientId);
    }

    @Test
    @DisplayName("응답 데이터가 없으면 빈 후보 목록을 반환한다")
    void returnsEmptySetWhenResponseDataIsNull() {

        UUID patientId = UUID.randomUUID();

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenReturn(
                ApiResponse.ok(
                        "사회복지사 정보 조회 성공",
                        null
                )
        );

        Set<UUID> result =
                adapter.findMatchableSocialWorkerIds(
                        patientId
                );

        assertEquals(
                Set.of(),
                result
        );
    }

    @Test
    @DisplayName("User-Service 4xx 오류는 재시도하지 않는다")
    void clientErrorDoesNotRetry() {

        UUID patientId = UUID.randomUUID();

        FeignException clientError =
                mock(FeignException.class);

        when(
                clientError.status()
        ).thenReturn(404);

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenThrow(
                clientError
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                FeignException.class,
                () ->
                        adapter.findMatchableSocialWorkerIds(
                                patientId
                        )
        );

        verify(
                userServiceClient,
                times(1)
        ).getMatchableSocialWorkers(patientId);
    }

    @Test
    @DisplayName("User-Service 5xx 오류 후 재시도 성공하면 후보 목록을 반환한다")
    void serverErrorRetriesOnceAndSucceeds() {

        UUID patientId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        FeignException serverError =
                mock(FeignException.class);

        when(
                serverError.status()
        ).thenReturn(500);

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        )
                .thenThrow(serverError)
                .thenReturn(
                        ApiResponse.ok(
                                "사회복지사 정보 조회 성공",
                                new UserMatchableSocialWorkersResponse(
                                        Set.of(workerId)
                                )
                        )
                );

        Set<UUID> result =
                adapter.findMatchableSocialWorkerIds(
                        patientId
                );

        assertEquals(
                Set.of(workerId),
                result
        );

        verify(
                userServiceClient,
                times(2)
        ).getMatchableSocialWorkers(patientId);
    }

    @Test
    @DisplayName("User-Service 5xx 오류가 계속되면 1회 재시도 후 실패한다")
    void repeatedServerErrorFailsAfterOneRetry() {

        UUID patientId = UUID.randomUUID();

        FeignException serverError =
                mock(FeignException.class);

        when(
                serverError.status()
        ).thenReturn(500);

        when(
                userServiceClient
                        .getMatchableSocialWorkers(patientId)
        ).thenThrow(
                serverError
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                FeignException.class,
                () ->
                        adapter.findMatchableSocialWorkerIds(
                                patientId
                        )
        );

        verify(
                userServiceClient,
                times(2)
        ).getMatchableSocialWorkers(patientId);
    }
}