package com.tissue.security.adapter.web.response;

import com.tissue.security.application.dto.OidcLoginResult;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.port.oidc.OidcTokenResult;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "OIDC device login poll result")
public record DevicePollResponse(
        @Schema(description = "`COMPLETE` / `PENDING` / `SLOW_DOWN` / `DENIED` / `EXPIRED` / `ERROR`")
        String status,

        @Schema(description = "Tissue access token (present only when `COMPLETE`)") @Nullable
        String accessToken,

        @Schema(description = "Tissue refresh token (present only when `COMPLETE`)") @Nullable
        String refreshToken) {

    public static DevicePollResponse from(OidcLoginResult result) {
        TokenPair tokens = result.tokens();
        if (result.status() == OidcTokenResult.Status.COMPLETE && tokens != null) {
            return new DevicePollResponse(result.status().name(), tokens.accessToken(), tokens.refreshToken());
        }
        return new DevicePollResponse(result.status().name(), null, null);
    }
}
