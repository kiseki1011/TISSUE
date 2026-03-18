package com.tissue.feature.system.web;

import com.tissue.config.SecurityProperties;
import com.tissue.config.SignupProperties;
import com.tissue.feature.system.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system-info")
@RequiredArgsConstructor
public class SystemInfoController {

    private final SystemProperties systemProperties;
    private final SignupProperties signupProperties;
    private final SecurityProperties securityProperties;

    @GetMapping
    public ResponseEntity<SystemInfoResponse> getSystemInfo() {
        SystemInfoResponse response = SystemInfoResponse.from(systemProperties, signupProperties, securityProperties);

        return ResponseEntity.ok(response);
    }
}
