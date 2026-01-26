package com.tissue.member.adapter.in.web;

import com.tissue.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.member.adapter.in.web.dto.request.EmailVerificationRequest;
import com.tissue.member.application.service.MemberEmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/verification")
@RequiredArgsConstructor
public class MemberEmailVerificationController {

    private final MemberEmailVerificationService memberEmailVerificationService;
    private final EmailVerificationProperties properties;

    @PostMapping("/request")
    public ResponseEntity<Void> requestVerification(@RequestBody @Valid EmailVerificationRequest request) {
        memberEmailVerificationService.sendVerificationEmail(request.email());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String email, @RequestParam String token) {
        boolean verified = memberEmailVerificationService.verifyEmail(email, token);

        // TODO: what is the use for the redirect url?
        //  send the user to the designated url after the user clicks the verification link?
        //  in this case shouldnt i create a thymeleaf page for each url?

        return ResponseEntity.ok(verified ? "Verification Succeeded!" : "Verification Failed.");

        //        String redirectUrl = verified ? properties.getSuccessUrl() : properties.getFailureUrl();
        //
        //        if (verified) {
        //            redirectUrl = UriComponentsBuilder.fromUriString(redirectUrl)
        //                    .queryParam("email", email)
        //                    .queryParam("token", token)
        //                    .build()
        //                    .toUriString();
        //        }
        //
        //        return ResponseEntity.status(HttpStatus.FOUND)
        //                .header(HttpHeaders.LOCATION, redirectUrl)
        //                .build();
    }

    @GetMapping("/verify-status")
    public ResponseEntity<Boolean> checkVerification(@RequestParam String email) {
        boolean verified = memberEmailVerificationService.isEmailVerified(email);
        return ResponseEntity.ok(verified);
    }
}
