package com.tissue.member.adapter.in.web.config;

import com.tissue.member.domain.policy.MemberPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberPolicyConfig {

    @Bean
    public MemberPolicy memberPolicy(
            @Value("${tissue.member.policy.max-owned-workspaces:10}") int maxOwned,
            @Value("${tissue.member.policy.max-joined-workspaces:10}") int maxJoined) {
        return new MemberPolicy(maxOwned, maxJoined);
    }
}
