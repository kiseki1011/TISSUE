package com.tissue.security.authentication.presentation;

import com.tissue.member.adapter.in.web.dto.request.PermissionRequest;
import com.tissue.security.authentication.application.port.in.AuthenticationUseCase;
import com.tissue.security.authentication.domain.MemberUserDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
import com.tissue.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.security.authentication.presentation.dto.request.RefreshTokenRequest;
import com.tissue.security.authentication.presentation.dto.response.ElevatedTokenResponse;
import com.tissue.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.security.authentication.presentation.dto.response.RefreshTokenResponse;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationUseCase.login(request.loginEmail(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authenticationUseCase.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token/elevate")
    public ResponseEntity<ElevatedTokenResponse> elevatePermission(
            @RequestBody @Valid PermissionRequest request, @CurrentMember MemberUserDetails userDetails) {
        ElevatedTokenResponse response = authenticationUseCase.elevatePermission(
                userDetails.getEmail(), request.password(), userDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentMember MemberUserDetails userDetails) {
        authenticationUseCase.logout(userDetails.getEmail());
        return ResponseEntity.noContent().build();
    }
}
