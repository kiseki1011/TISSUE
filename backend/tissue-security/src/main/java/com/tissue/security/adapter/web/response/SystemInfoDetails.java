package com.tissue.security.adapter.web.response;

import com.tissue.feature.member.config.MemberDeletionProperties;
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

        @Schema(
                description = "Days a withdrawn member's account is kept before PII is anonymized. "
                        + "Clients can use this to display the restore window.",
                example = "7")
        long memberDeletionRetentionDays,

        @Schema(description = "Server setup configuration") Setup setup) {

    @Schema(description = "Server setup configuration details")
    @Builder
    public record Setup(
            @Schema(description = "Whether new member registration is allowed")
            boolean allowSignup,

            @Schema(description = "Whether email is required for authentication")
            boolean emailRequired,

            @Schema(description = "Available authentication providers")
            List<String> authProviders) {}

    public static SystemInfoDetails from(
            SystemProperties systemProperties,
            SignupProperties signupProperties,
            TissueSecurityProperties tissueSecurityProperties,
            MemberDeletionProperties memberDeletionProperties,
            List<String> authProviders) {
        return SystemInfoDetails.builder()
                .version(systemProperties.getVersion())
                .serverName(systemProperties.getServerName())
                .memberDeletionRetentionDays(
                        memberDeletionProperties.getRetention().toDays())
                .setup(Setup.builder()
                        .allowSignup(signupProperties.isEnabled())
                        .emailRequired(tissueSecurityProperties.isEmailRequired())
                        .authProviders(authProviders)
                        .build())
                .build();
    }
}
