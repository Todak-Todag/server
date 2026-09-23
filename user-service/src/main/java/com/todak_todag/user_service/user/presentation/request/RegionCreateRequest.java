package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.RegionCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionCreateRequest(

        @NotBlank(message = "시/도는 필수입니다.")
        @Size(max = 20, message = "시/도는 최대 20자입니다.")
        String province,

        @NotBlank(message = "시/군/구는 필수입니다.")
        @Size(max = 20, message = "시/군/구는 최대 20자입니다.")
        String district,

        @NotBlank(message = "행정구역 코드는 필수입니다.")
        @Size(max = 20, message = "행정구역 코드는 최대 20자입니다.")
        String regionCode

        /**
         * isActive의 경우
         * 지역이 등록될 때 false를 기본값으로 저장
         * 즉, 첫 지역 생성시 비활성화로 생성되고,
         * 이후 관리자의 검토 이후 '지역 상태 활성/비활성 변경 API'를 사용
         */
) {

    public RegionCreateCommand toCommand() {
        return new RegionCreateCommand(
                province,
                district,
                regionCode
        );
    }
}