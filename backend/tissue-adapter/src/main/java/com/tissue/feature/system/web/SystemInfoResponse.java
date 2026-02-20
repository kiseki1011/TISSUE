package com.tissue.feature.system.web;

import com.tissue.config.SecurityProperties;
import com.tissue.config.SignupProperties;
import com.tissue.support.system.SystemProperties;
import java.util.List;
import lombok.Builder;

@Builder
public record SystemInfoResponse(String serverName, Setup setup) {

    @Builder
    public record Setup(boolean allowSignup, boolean domainRestricted, List<String> authProviders) {}

    public static SystemInfoResponse from(
            SystemProperties systemProperties,
            SignupProperties signupProperties,
            SecurityProperties securityProperties) {

        return SystemInfoResponse.builder()
                .serverName(systemProperties.getServerName())
                .setup(Setup.builder()
                        .allowSignup(signupProperties.isAllowSignup())
                        .domainRestricted(signupProperties.isDomainRestricted())
                        .authProviders(securityProperties.getAuthProviders())
                        .build())
                .build();
    }
}
