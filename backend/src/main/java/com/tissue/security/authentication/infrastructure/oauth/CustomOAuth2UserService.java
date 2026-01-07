package com.tissue.security.authentication.infrastructure.oauth;

import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.security.authentication.domain.MemberDetails;
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
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo oAuth2UserInfo =
                switch (provider) {
                    case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
                    case GITHUB -> new GithubOAuth2UserInfo(attributes);
                    default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
                };

        Member member = authIdentityRepository
                .findByProviderAndIdentifier(provider, oAuth2UserInfo.getProviderId())
                .map(AuthIdentity::getMember)
                .map(m -> updateMember(m, oAuth2UserInfo))
                .orElseThrow(() -> new OAuth2AuthenticationException("Member not found"));

        return new MemberDetails(member, attributes);
    }

    private Member updateMember(Member member, OAuth2UserInfo userInfo) {
        // TODO: update info if needed
        return member;
    }
}
