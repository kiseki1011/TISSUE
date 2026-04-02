package com.tissue.security.adapter.web;

import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.config.SignupProperties;
import com.tissue.security.config.SystemProperties;
import com.tissue.security.config.TissueSecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System Info")
@RestController
@RequestMapping("/api/v1/system-info")
@RequiredArgsConstructor
public class SystemInfoController {

    private final SystemProperties systemProperties;
    private final SignupProperties signupProperties;
    private final TissueSecurityProperties tissueSecurityProperties;

    @Operation(
            summary = "Get system info",
            description = "Retrieve the server's public configuration"
                    + " including signup settings and available auth providers.")
    @ApiResponse(responseCode = "200", description = "System info retrieved")
    @PublicApi
    @GetMapping
    public ResponseEntity<SystemInfoResponse> getSystemInfo() {
        SystemInfoResponse response =
                SystemInfoResponse.from(systemProperties, signupProperties, tissueSecurityProperties);

        return ResponseEntity.ok(response);
    }
}
