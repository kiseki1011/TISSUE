package com.tissue.adapter.web;

import com.tissue.adapter.web.request.LoginRequest;
import com.tissue.adapter.web.request.PermissionRequest;
import com.tissue.adapter.web.request.RefreshTokenRequest;
import com.tissue.application.dto.response.ElevatedTokenResponse;
import com.tissue.application.dto.response.LoginResponse;
import com.tissue.application.dto.response.RefreshTokenResponse;
import com.tissue.application.port.usecase.AuthenticationUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationUseCase authenticationUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        LoginResponse response =
                authenticationUseCase.login(request.loginEmail(), request.password(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {

        RefreshTokenResponse response = authenticationUseCase.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token/elevate")
    public ResponseEntity<ElevatedTokenResponse> elevatePermission(
            @RequestBody @Valid PermissionRequest request, @CurrentMember MemberDetails userDetails) {

        ElevatedTokenResponse response =
                authenticationUseCase.elevatePermission(userDetails.getEmail(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentMember MemberDetails userDetails) {

        authenticationUseCase.logout(userDetails.getEmail());
        return ResponseEntity.noContent().build();
    }
}
