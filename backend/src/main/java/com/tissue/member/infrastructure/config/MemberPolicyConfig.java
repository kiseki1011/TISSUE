package com.tissue.member.infrastructure.config;

import com.tissue.member.domain.policy.MemberPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberPolicyConfig {

    @Bean
    public MemberPolicy memberPolicy(MemberProperties properties) {
        return new MemberPolicy(properties.getMaxOwnedWorkspaces(), properties.getMaxJoinedWorkspaces());
    }
}
