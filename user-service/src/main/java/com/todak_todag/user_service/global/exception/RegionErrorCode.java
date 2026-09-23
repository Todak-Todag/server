package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RegionErrorCode implements ErrorCode {

    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다."),
    REGION_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 행정구역 코드입니다."),
    REGION_UPDATE_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "수정할 지역 정보를 하나 이상 입력해야 합니다."),
    REGION_UPDATE_VALUE_INVALID(HttpStatus.BAD_REQUEST, "지역 정보는 빈 값으로 수정할 수 없습니다.");


    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {return name();}

}