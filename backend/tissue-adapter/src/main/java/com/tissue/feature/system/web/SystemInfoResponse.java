package com.tissue.feature.system.web;

import com.tissue.feature.system.config.SystemProperties;
import com.tissue.security.config.SignupProperties;
import com.tissue.security.config.TissueSecurityProperties;
import java.util.List;
import lombok.Builder;

@Builder
public record SystemInfoResponse(String serverName, Setup setup) {

    @Builder
    public record Setup(
            boolean allowSignup, boolean emailRequired, boolean domainRestricted, List<String> authProviders) {}

    public static SystemInfoResponse from(
            SystemProperties systemProperties,
            SignupProperties signupProperties,
            TissueSecurityProperties tissueSecurityProperties) {
        return SystemInfoResponse.builder()
                .serverName(systemProperties.getServerName())
                .setup(Setup.builder()
                        .allowSignup(signupProperties.isEnabled())
                        .emailRequired(tissueSecurityProperties.isEmailRequired())
                        .domainRestricted(signupProperties.isDomainRestricted())
                        .authProviders(tissueSecurityProperties.getAuthProviders())
                        .build())
                .build();
    }
}
