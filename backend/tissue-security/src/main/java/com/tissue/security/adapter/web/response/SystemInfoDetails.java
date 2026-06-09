package com.tissue.security.adapter.web.response;

import com.tissue.feature.member.config.MemberDeletionProperties;
import com.tissue.security.config.SystemProperties;
import com.tissue.security.config.TissueAuthProperties;
import com.tissue.security.config.TissueSecurityProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

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

            @Schema(description = "Instance authentication mode", example = "LOCAL")
            String authMode,

            @Schema(description = "Available authentication providers")
            List<String> authProviders,

            @Schema(description = "Identity provider details; present only when authMode is OIDC") @Nullable
            Oidc oidc) {}

    @Schema(description = "OIDC identity provider details")
    public record Oidc(
            @Schema(description = "OIDC issuer URI") String issuerUri,

            @Schema(description = "OAuth2 client id registered at the IdP")
            String clientId,

            @Schema(description = "IdP name", example = "Keycloak")
            String providerName) {}

    public static SystemInfoDetails from(
            SystemProperties systemProperties,
            boolean allowSignup,
            TissueSecurityProperties tissueSecurityProperties,
            MemberDeletionProperties memberDeletionProperties,
            TissueAuthProperties authProperties,
            List<String> authProviders) {
        Setup.SetupBuilder setup = Setup.builder()
                .allowSignup(allowSignup)
                .emailRequired(tissueSecurityProperties.isEmailRequired())
                .authMode(authProperties.getMode().name())
                .authProviders(authProviders);
        if (authProperties.getMode() == TissueAuthProperties.Mode.OIDC) {
            setup.oidc(new Oidc(
                    authProperties.getOidc().getIssuerUri(),
                    authProperties.getOidc().getClientId(),
                    authProperties.getOidc().getProviderName()));
        }
        return SystemInfoDetails.builder()
                .version(systemProperties.getVersion())
                .serverName(systemProperties.getServerName())
                .memberDeletionRetentionDays(
                        memberDeletionProperties.getRetention().toDays())
                .setup(setup.build())
                .build();
    }
}
