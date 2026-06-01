package com.tissue.security.principal;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.shared.auth.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final AuthenticationIdentityRepository authenticationIdentityRepository;
    private final TissueSecurityProperties tissueSecurityProperties;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        AuthenticationIdentityProvider provider = tissueSecurityProperties.isEmailRequired()
                ? AuthenticationIdentityProvider.EMAIL
                : AuthenticationIdentityProvider.USERNAME;

        AuthenticationIdentity authenticationIdentity = authenticationIdentityRepository
                .findByProviderAndIdentifier(provider, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found for identifier: " + identifier));

        Member member = authenticationIdentity.getMember();

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new UsernameNotFoundException("Member is not active: " + identifier);
        }

        return new MemberDetails(member, authenticationIdentity.getCredential());
    }
}
