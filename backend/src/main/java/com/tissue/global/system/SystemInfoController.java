package com.tissue.global.system;

import com.tissue.member.adapter.in.web.config.MemberProperties;
import com.tissue.security.config.SecurityProperties;
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
    private final MemberProperties memberProperties;
    private final SecurityProperties securityProperties;

    @GetMapping
    public ResponseEntity<SystemInfoResponse> getSystemInfo() {
        SystemInfoResponse response = SystemInfoResponse.builder()
                .status("UP")
                .serverName(systemProperties.getServerName())
                .setup(SystemInfoResponse.Setup.builder()
                        .mode(systemProperties.getMode())
                        .allowSignup(memberProperties.isAllowSignup())
                        .authProviders(securityProperties.getAuthProviders())
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }
}
