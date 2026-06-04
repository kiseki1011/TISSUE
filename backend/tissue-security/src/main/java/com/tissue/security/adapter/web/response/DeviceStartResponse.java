package com.tissue.security.adapter.web.response;

import com.tissue.security.application.port.oidc.OidcDeviceAuthorization;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "OIDC device login start. Show the user code at the verification URI, then poll `/device:poll`.")
public record DeviceStartResponse(
        @Schema(description = "Code the user enters at the verification URI", example = "XYZW-DCBA")
        String userCode,

        @Schema(description = "Where the user authenticates")
        String verificationUri,

        @Schema(description = "Verification URI with the user code embedded (optional)") @Nullable
        String verificationUriComplete,

        @Schema(description = "Opaque code the client passes back to `/device:poll`")
        String deviceCode,

        @Schema(description = "Minimum seconds between polls")
        int interval,

        @Schema(description = "Seconds until the device code expires")
        int expiresIn) {

    public static DeviceStartResponse from(OidcDeviceAuthorization auth) {
        return new DeviceStartResponse(
                auth.userCode(),
                auth.verificationUri(),
                auth.verificationUriComplete(),
                auth.deviceCode(),
                auth.interval(),
                auth.expiresIn());
    }
}
