package com.tissue.security.adapter.web;

import com.tissue.security.config.SignupProperties;
import com.tissue.security.config.SystemProperties;
import com.tissue.security.config.TissueSecurityProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Server system information")
@Builder
public record SystemInfoResponse(
        @Schema(description = "Server display name", example = "My Tissue Server")
        String serverName,

        @Schema(description = "Server setup configuration") Setup setup) {

    @Schema(description = "Server setup configuration details")
    @Builder
    public record Setup(
            @Schema(description = "Whether new member registration is allowed")
            boolean allowSignup,

            @Schema(description = "Whether email is required for authentication")
            boolean emailRequired,

            @Schema(description = "Whether signup is restricted to specific email domains")
            boolean domainRestricted,

            @Schema(description = "Available authentication providers")
            List<String> authProviders) {}

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
