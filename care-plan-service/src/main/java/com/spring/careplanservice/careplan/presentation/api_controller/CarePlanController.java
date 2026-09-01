package com.spring.careplanservice.careplan.presentation.api_controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CarePlanController {
    @GetMapping("/test")
    public String test() {
        return "test 성공";
    }
}
