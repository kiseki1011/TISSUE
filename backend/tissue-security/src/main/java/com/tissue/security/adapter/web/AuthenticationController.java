package com.tissue.security.adapter.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.global.openapi.AuthenticationErrors;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.request.LoginRequest;
import com.tissue.security.adapter.web.request.RefreshTokenRequest;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.usecase.AuthenticationUseCase;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationUseCase authenticationUseCase;

    @Operation(operationId = "login", summary = "Login", description = """
                Authenticate with identifier and password to obtain JWT tokens.\
                 The identifier is either `email` or `username` depending \
                on the server's `email-required` setting.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.LOGIN_RATE_LIMITED})
    @PublicApi
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response =
                authenticationUseCase.login(request.identifier(), request.password(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "refreshToken",
            summary = "Refresh token",
            description = "Issue a new access token and refresh token using an existing refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @AuthenticationErrors({
        AuthenticationErrorCode.INVALID_TOKEN,
        AuthenticationErrorCode.EXPIRED_TOKEN,
        AuthenticationErrorCode.REFRESH_TOKEN_NOT_FOUND,
        AuthenticationErrorCode.TOKEN_REUSE_DETECTED,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
    })
    @PublicApi
    @PostMapping("/token:refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        RefreshTokenResponse response = authenticationUseCase.refreshToken(request.refreshToken());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "logout",
            summary = "Logout",
            description = "Revoke the refresh token for the current logged in member.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentMember MemberDetails memberDetails) {
        authenticationUseCase.logout(memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
