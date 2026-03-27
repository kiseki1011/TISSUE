package com.tissue.security.oauth2;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.oauth2.userinfo.GithubOAuth2UserInfo;
import com.tissue.security.oauth2.userinfo.GoogleOAuth2UserInfo;
import com.tissue.security.oauth2.userinfo.OAuth2UserInfo;
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

    private final AuthenticationIdentityRepository authenticationIdentityRepository;

    @Override
    @Transactional(readOnly = true)
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthenticationIdentityProvider provider = AuthenticationIdentityProvider.fromRegistrationId(registrationId);
        Map<String, Object> attributes = oauth2User.getAttributes();

        OAuth2UserInfo oauth2UserInfo =
                switch (provider) {
                    case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
                    case GITHUB -> new GithubOAuth2UserInfo(attributes);
                    default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
                };

        Member member = authenticationIdentityRepository
                .findByProviderAndIdentifier(provider, oauth2UserInfo.getProviderId())
                .map(AuthenticationIdentity::getMember)
                .orElse(null);

        return new CustomOAuth2User(member, oauth2UserInfo);
    }
}
