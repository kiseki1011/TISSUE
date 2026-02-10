package com.tissue.global.security.principal;

import com.tissue.feature.member.application.port.out.AuthIdentityRepository;
import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service to load user-specific data during authentication.
 *
 * <p>This service implements Spring Security's {@link UserDetailsService}.
 * It retrieves the {@link AuthIdentity} for the given email (username) from the repository
 * and constructs a {@link MemberDetails} object that contains both the {@link Member} information
 * and the secure credential (password).
 */
@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final AuthIdentityRepository authIdentityRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AuthIdentity authIdentity = authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, email)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found for email: " + email));

        Member member = authIdentity.getMember();

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new UsernameNotFoundException("Member is not active: " + email);
        }

        return new MemberDetails(member, authIdentity.getCredential());
    }
}
