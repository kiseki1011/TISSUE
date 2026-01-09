package com.tissue.security.authentication.infrastructure.oauth;

import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface OAuth2UserInfo {
    String getProviderId();

    String getProvider();

    @Nullable
    String getEmail();

    @Nullable
    String getName();

    Map<String, Object> getAttributes();
}
