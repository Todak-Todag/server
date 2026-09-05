package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.response.PageResponse;
import com.todak_todag.user_service.user.application.result.*;
import com.todak_todag.user_service.user.application.service.command.RegionCommandService;
import com.todak_todag.user_service.user.application.service.query.RegionQueryService;
import com.todak_todag.user_service.user.presentation.request.RegionCreateRequest;
import com.todak_todag.user_service.user.presentation.request.RegionFindAdminRequest;
import com.todak_todag.user_service.user.presentation.request.RegionUpdateActiveRequest;
import com.todak_todag.user_service.user.presentation.request.RegionUpdateRequest;
import com.todak_todag.user_service.user.presentation.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RegionController implements RegionApiSpec {

    private final RegionQueryService regionQueryService;
    private final RegionCommandService regionCommandService;

    // 회원가입용 서비스 가능 지역 목록 조회
    @Override
    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<RegionFindAvailableListResponse>> findAvailableRegions() {

        List<RegionFindAvailableResult> results =
                regionQueryService.findAvailableRegions();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "서비스 가능 지역 목록 조회 성공",
                                RegionFindAvailableListResponse.of(results)
                        )
                );
    }

    // 관리자 지역 목록 조회
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/admin/regions")
    public ResponseEntity<ApiResponse<PageResponse<RegionFindAdminResponse>>> findAdminRegions(
            @Valid @ModelAttribute RegionFindAdminRequest request
    ) {
        Page<RegionFindAdminResult> results =
                regionQueryService.findAdminRegions(request.toQuery());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "지역 목록 조회 성공",
                                PageResponse.of(
                                        results,
                                        RegionFindAdminResponse::from
                                )
                        )
                );
    }

    @Override
    @GetMapping("/regions/{regionId}")
    public ResponseEntity<ApiResponse<RegionFindDetailResponse>> findRegion(
            @PathVariable UUID regionId
    ) {
        RegionFindDetailResult result =
                regionQueryService.findRegion(regionId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "지역 상세 조회 성공",
                                RegionFindDetailResponse.from(result)
                        )
                );
    }

    // 관리자 지역 등록
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @PostMapping("/admin/regions")
    public ResponseEntity<ApiResponse<RegionCreateResponse>> createRegion(
            @Valid @RequestBody RegionCreateRequest request
    ) {
        RegionCreateResult result =
                regionCommandService.createRegion(request.toCommand());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "지역 등록 성공",
                                RegionCreateResponse.from(result)
                        )
                );
    }

    // 활성/비활성 지역 상태 변경
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @PatchMapping("/admin/regions/{regionId}/active")
    public ResponseEntity<ApiResponse<RegionUpdateActiveResponse>> updateActive(
            @PathVariable UUID regionId,
            @Valid @RequestBody RegionUpdateActiveRequest request
    ) {
        RegionUpdateActiveResult result =
                regionCommandService.updateActive(
                        request.toCommand(regionId)
                );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "지역 상태 변경 성공",
                        RegionUpdateActiveResponse.from(result)
                )
        );
    }

    // 지역 정보 수정(관리자)
    @Override
    @PreAuthorize("hasRole('MASTER')")
    @PatchMapping("/admin/regions/{regionId}")
    public ResponseEntity<ApiResponse<RegionUpdateResponse>> updateRegion(
            @PathVariable UUID regionId,
            @Valid @RequestBody RegionUpdateRequest request
    ) {
        RegionUpdateResult result =
                regionCommandService.updateRegion(
                        request.toCommand(regionId)
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "지역 정보 수정 성공",
                                RegionUpdateResponse.from(result)
                        )
                );
    }
}