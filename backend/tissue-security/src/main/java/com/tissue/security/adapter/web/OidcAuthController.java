package com.tissue.security.adapter.web;

import com.tissue.global.openapi.AuthenticationErrors;
import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.request.DevicePollRequest;
import com.tissue.security.adapter.web.response.DevicePollResponse;
import com.tissue.security.adapter.web.response.DeviceStartResponse;
import com.tissue.security.application.dto.OidcLoginResult;
import com.tissue.security.application.port.oidc.OidcDeviceAuthorization;
import com.tissue.security.application.service.OidcLoginService;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.auth.OidcAuthOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth/oidc")
@RequiredArgsConstructor
public class OidcAuthController {

    private final ObjectProvider<OidcLoginService> oidcLoginService;

    @Operation(
            operationId = "startOidcDeviceLogin",
            summary = "Start OIDC device login",
            description = "Only available when the instance runs in OIDC authentication mode.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device authorization started"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.OIDC_AUTH_ONLY})
    @OidcAuthOnly
    @PublicApi
    @PostMapping("/device:start")
    public ResponseEntity<DeviceStartResponse> startDeviceLogin() {
        OidcDeviceAuthorization auth = oidcLoginService.getObject().startDeviceLogin();
        return ResponseEntity.ok(DeviceStartResponse.from(auth));
    }

    @Operation(
            operationId = "pollOidcDeviceLogin",
            summary = "Poll OIDC device login for Tissue tokens",
            description = "Only available when the instance runs in OIDC authentication mode.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Poll result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @AuthenticationErrors({
        AuthenticationErrorCode.OIDC_AUTH_ONLY,
        AuthenticationErrorCode.OIDC_PROVISIONING_DISABLED,
        AuthenticationErrorCode.OIDC_EMAIL_DOMAIN_NOT_ALLOWED,
        AuthenticationErrorCode.OIDC_EMAIL_MISSING,
        AuthenticationErrorCode.OIDC_EMAIL_CONFLICT,
    })
    @OidcAuthOnly
    @PublicApi
    @PostMapping("/device:poll")
    public ResponseEntity<DevicePollResponse> pollDeviceLogin(@Valid @RequestBody DevicePollRequest request) {
        OidcLoginResult result = oidcLoginService.getObject().completeDeviceLogin(request.deviceCode());
        return ResponseEntity.ok(DevicePollResponse.from(result));
    }
}
