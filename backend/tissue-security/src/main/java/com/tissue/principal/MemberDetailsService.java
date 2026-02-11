package com.tissue.principal;

import com.tissue.application.port.repository.AuthIdentityRepository;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
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
 * It retrieves the {@link AuthenticationIdentity} for the given email (username) from the repository
 * and constructs a {@link MemberDetails} object that contains both the {@link Member} information
 * and the secure credential (password).
 */
@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final AuthIdentityRepository authIdentityRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AuthenticationIdentity authenticationIdentity = authIdentityRepository
                .findByProviderAndIdentifier(AuthenticationProvider.EMAIL, email)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found for email: " + email));

        Member member = authenticationIdentity.getMember();

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new UsernameNotFoundException("Member is not active: " + email);
        }

        return new MemberDetails(member, authenticationIdentity.getCredential());
    }
}
