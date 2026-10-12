package com.hosiyar.lms.common.controller;

import com.hosiyar.lms.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smoke-test endpoint for episode 1 - proves the app boots and the
 * ApiResponse wrapper works before any real module exists yet.
 */
@RestController
public class PingController {

    @GetMapping("/api/v1/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("pong", "LMS backend is alive");
    }
}
