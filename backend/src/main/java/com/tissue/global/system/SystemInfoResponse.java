package com.tissue.global.system;

import com.tissue.member.adapter.in.web.config.MemberProperties;
import com.tissue.security.config.SecurityProperties;
import java.util.List;
import lombok.Builder;

@Builder
public record SystemInfoResponse(String status, String serverName, Setup setup) {

    @Builder
    public record Setup(SystemProperties.Mode mode, boolean allowSignup, List<String> authProviders) {}

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
