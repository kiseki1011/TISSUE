package com.tissue.security.application.port.usecase;

public interface MemberAccountUseCase {

    void linkEmailAuthentication(String newPassword, Long memberId);

    void linkOAuthAccount(String registerToken, Long memberId);

    void updateUsername(String newUsername, Long memberId);

    void updateEmail(String newEmail, String verificationToken, Long memberId);

    void updatePassword(String originalPassword, String newPassword, Long memberId);

    void withdraw(String password, Long memberId);

    void checkEmailAvailability(String email);

    void checkUsernameAvailability(String username);
}
