package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.application.service.query.RegionQueryService;
import com.todak_todag.user_service.user.presentation.response.RegionFindAvailableListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController implements RegionApiSpec {

    private final RegionQueryService regionQueryService;

    // 회원가입용 서비스 가능 지역 목록 조회
    @Override
    @GetMapping
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
}