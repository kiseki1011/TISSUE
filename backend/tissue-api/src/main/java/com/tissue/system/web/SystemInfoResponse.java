package com.tissue.system.web;

import com.tissue.feature.member.config.MemberProperties;
import com.tissue.global.security.config.SecurityProperties;
import com.tissue.support.system.Mode;
import com.tissue.support.system.SystemProperties;
import java.util.List;
import lombok.Builder;

// TODO: Add version of system
@Builder
public record SystemInfoResponse(String status, String serverName, Setup setup) {

    @Builder
    public record Setup(Mode mode, boolean allowSignup, List<String> authProviders) {}

    public static SystemInfoResponse from(
            SystemProperties systemProperties,
            MemberProperties memberProperties,
            SecurityProperties securityProperties) {

        return SystemInfoResponse.builder()
                .status("UP")
                .serverName(systemProperties.getServerName())
                .setup(Setup.builder()
                        .mode(systemProperties.getMode())
                        .allowSignup(memberProperties.isAllowSignup())
                        .authProviders(securityProperties.getAuthProviders())
                        .build())
                .build();
    }
}
