package com.tissue.security.authentication.infrastructure.oauth;

import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthIdentityRepository authIdentityRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));

        String userNameAttributeName = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserProfile userProfile = OAuth2UserProfile.of(provider, attributes);

        Member member = authIdentityRepository
                .findByProviderAndIdentifier(provider, userProfile.identifier())
                .map(AuthIdentity::getMember)
                .map(m -> updateMember(m, userProfile))
                .orElse(null);

        return new CustomOAuth2User(
                member,
                attributes,
                userNameAttributeName,
                provider.name(),
                userProfile.identifier(),
                userProfile.email());
    }

    private Member updateMember(Member member, OAuth2UserProfile userProfile) {
        // TODO: update info if needed
        return member;
    }
}
