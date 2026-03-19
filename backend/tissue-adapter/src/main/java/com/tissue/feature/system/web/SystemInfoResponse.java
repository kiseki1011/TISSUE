package com.tissue.feature.system.web;

import com.tissue.feature.system.config.SystemProperties;
import com.tissue.security.config.SecurityProperties;
import com.tissue.security.config.SignupProperties;
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
                        .allowSignup(signupProperties.isEnabled())
                        .domainRestricted(signupProperties.isDomainRestricted())
                        .authProviders(securityProperties.getAuthProviders())
                        .build())
                .build();
    }
}
