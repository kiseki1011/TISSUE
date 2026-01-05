package com.tissue.security.authentication.infrastructure.oauth;

import com.tissue.member.domain.AuthProvider;
import java.util.Map;

public record OAuth2UserProfile(String identifier, String email, String name) {
    public static OAuth2UserProfile of(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> ofGoogle(attributes);
            case GITHUB -> ofGithub(attributes);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    private static OAuth2UserProfile ofGoogle(Map<String, Object> attributes) {
        return new OAuth2UserProfile(
                (String) attributes.getOrDefault("sub", ""), (String) attributes.getOrDefault("email", ""), (String)
                        attributes.getOrDefault("name", ""));
    }

    private static OAuth2UserProfile ofGithub(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String login = (String) attributes.get("login");

        return new OAuth2UserProfile(
                String.valueOf(attributes.get("id")), email != null ? email : "", login != null ? login : "");
    }
}
