package com.tissue.security.adapter.web;

import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.request.DevicePollRequest;
import com.tissue.security.adapter.web.response.DevicePollResponse;
import com.tissue.security.adapter.web.response.DeviceStartResponse;
import com.tissue.security.application.dto.OidcLoginResult;
import com.tissue.security.application.port.oidc.OidcDeviceAuthorization;
import com.tissue.security.application.service.OidcLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth/oidc")
@ConditionalOnProperty(name = "tissue.auth.mode", havingValue = "OIDC")
@RequiredArgsConstructor
public class OidcAuthController {

    private final OidcLoginService oidcLoginService;

    @Operation(operationId = "startOidcDeviceLogin", summary = "Start OIDC device login")
    @PublicApi
    @PostMapping("/device:start")
    public ResponseEntity<DeviceStartResponse> startDeviceLogin() {
        OidcDeviceAuthorization auth = oidcLoginService.startDeviceLogin();
        return ResponseEntity.ok(DeviceStartResponse.from(auth));
    }

    @Operation(operationId = "pollOidcDeviceLogin", summary = "Poll OIDC device login for Tissue tokens")
    @PublicApi
    @PostMapping("/device:poll")
    public ResponseEntity<DevicePollResponse> pollDeviceLogin(@Valid @RequestBody DevicePollRequest request) {
        OidcLoginResult result = oidcLoginService.completeDeviceLogin(request.deviceCode());
        return ResponseEntity.ok(DevicePollResponse.from(result));
    }
}
