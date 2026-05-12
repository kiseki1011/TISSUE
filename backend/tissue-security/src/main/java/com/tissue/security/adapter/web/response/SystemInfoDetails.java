package com.tissue.security.adapter.web.response;

import com.tissue.security.config.DeploymentProperties;
import com.tissue.security.config.SignupProperties;
import com.tissue.security.config.SystemProperties;
import com.tissue.security.config.TissueSecurityProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Server system information")
@Builder
public record SystemInfoDetails(
        @Schema(description = "Tissue server version", example = "0.7.0")
        String version,

        @Schema(description = "Server display name", example = "My Tissue Server")
        String serverName,

        @Schema(description = "Whether the server runs in multi-tenant (SaaS) deployment mode")
        boolean multiTenant,

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

    public static SystemInfoDetails from(
            SystemProperties systemProperties,
            SignupProperties signupProperties,
            TissueSecurityProperties tissueSecurityProperties,
            DeploymentProperties deploymentProperties,
            List<String> authProviders) {
        return SystemInfoDetails.builder()
                .version(systemProperties.getVersion())
                .serverName(systemProperties.getServerName())
                .multiTenant(deploymentProperties.isMultiTenant())
                .setup(Setup.builder()
                        .allowSignup(signupProperties.isEnabled()
                                && (signupProperties.isAllDomainsAllowed() || signupProperties.isDomainRestricted()))
                        .emailRequired(tissueSecurityProperties.isEmailRequired())
                        .domainRestricted(signupProperties.isDomainRestricted())
                        .authProviders(authProviders)
                        .build())
                .build();
    }
}
