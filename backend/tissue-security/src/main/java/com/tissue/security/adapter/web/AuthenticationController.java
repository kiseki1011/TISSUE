package com.tissue.security.adapter.web;

import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.request.LoginRequest;
import com.tissue.security.adapter.web.request.PermissionRequest;
import com.tissue.security.adapter.web.request.RefreshTokenRequest;
import com.tissue.security.application.dto.response.ElevatedTokenResponse;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;
import com.tissue.security.application.port.usecase.AuthenticationUseCase;
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

    @Operation(
            summary = "Login",
            description = "Authenticate with identifier and password to obtain JWT tokens."
                    + " The identifier is either `email` or `username` depending "
                    + "on the server's `email-required` setting.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "429", description = "Too many login attempts", content = @Content)
    })
    @PublicApi
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response =
                authenticationUseCase.login(request.identifier(), request.password(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh token",
            description = "Issue a new access token and refresh token using an existing refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(
                responseCode = "401",
                description = "Refresh token is invalid, expired, or reused",
                content = @Content)
    })
    @PublicApi
    @PostMapping("/token:refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authenticationUseCase.refreshToken(request.refreshToken());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Elevate permission",
            description = "Authenticate to obtain a short-lived elevated token for sensitive operations"
                    + " such as password change or account deletion.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permission elevated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "429", description = "Too many attempts", content = @Content)
    })
    @PostMapping("/token:elevate")
    public ResponseEntity<ElevatedTokenResponse> elevatePermission(
            @RequestBody @Valid PermissionRequest request,
            @CurrentMember MemberDetails userDetails,
            HttpServletRequest httpRequest) {
        String identifier = userDetails.getEmail() != null ? userDetails.getEmail() : userDetails.getUsername();
        ElevatedTokenResponse response =
                authenticationUseCase.elevatePermission(identifier, request.password(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout", description = "Revoke the refresh token for the current logged in member.")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentMember MemberDetails userDetails) {
        authenticationUseCase.logout(userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
