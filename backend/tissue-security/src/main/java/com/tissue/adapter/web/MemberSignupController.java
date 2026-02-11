package com.tissue.adapter.web;

import com.tissue.adapter.web.request.LinkOAuthAccountRequest;
import com.tissue.adapter.web.request.SignupMemberRequest;
import com.tissue.adapter.web.request.SignupOAuthMemberRequest;
import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.application.port.usecase.MemberSignupUseCase;
import com.tissue.feature.member.application.dto.response.MemberSignupResponse;
import com.tissue.principal.MemberDetails;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberSignupController {

    private final MemberSignupUseCase memberSignupUseCase;

    @PostMapping("/signup/email")
    public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody SignupMemberRequest request) {
        var command = request.toCommand();
        MemberSignupResponse response = memberSignupUseCase.signup(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{memberId}")
                .buildAndExpand(response.memberId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/signup/oauth")
    public ResponseEntity<OAuthSignupResponse> signupOAuth(@Valid @RequestBody SignupOAuthMemberRequest request) {
        OAuthSignupResponse response = memberSignupUseCase.signupOAuth(request.toCommand());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/link/oauth")
    public ResponseEntity<Void> linkOAuthAccount(
            @Valid @RequestBody LinkOAuthAccountRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberSignupUseCase.linkOAuthAccount(request.registerToken(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
