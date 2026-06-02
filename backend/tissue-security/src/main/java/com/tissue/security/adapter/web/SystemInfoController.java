package com.tissue.security.adapter.web;

import com.tissue.feature.member.config.MemberDeletionProperties;
import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.response.SystemInfoDetails;
import com.tissue.security.application.service.SignupGuardrails;
import com.tissue.security.config.SignupProperties;
import com.tissue.security.config.SystemProperties;
import com.tissue.security.config.TissueSecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
    private final SignupGuardrails signupGuardrails;
    private final TissueSecurityProperties tissueSecurityProperties;
    private final MemberDeletionProperties memberDeletionProperties;

    @Operation(operationId = "getSystemInfo", summary = "Get system info", description = """
                Retrieve the server's public configuration\
                 including signup settings and available auth providers.""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "System info retrieved")})
    @PublicApi
    @GetMapping
    public ResponseEntity<SystemInfoDetails> getSystemInfo() {
        List<String> authProviders = resolveAuthProviders();

        boolean allowSignup = signupProperties.isEnabled() || signupGuardrails.isFirstUser();
        SystemInfoDetails response = SystemInfoDetails.from(
                systemProperties, allowSignup, tissueSecurityProperties, memberDeletionProperties, authProviders);

        return ResponseEntity.ok(response);
    }

    private List<String> resolveAuthProviders() {
        if (tissueSecurityProperties.isEmailRequired()) {
            return List.of("EMAIL");
        }
        return List.of();
    }
}
