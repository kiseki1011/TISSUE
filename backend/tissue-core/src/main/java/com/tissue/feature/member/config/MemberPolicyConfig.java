package com.tissue.feature.member.config;

import com.tissue.feature.member.domain.policy.MemberPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberPolicyConfig {

    @Bean
    public MemberPolicy memberPolicy(MemberPolicyProperties properties) {
        return new MemberPolicy(properties.getMaxOwnedWorkspaces(), properties.getMaxJoinedWorkspaces());
    }
}
