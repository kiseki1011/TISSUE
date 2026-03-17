package com.tissue.feature.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.member.policy")
public class MemberPolicyProperties {

    private int maxOwnedWorkspaces = 10;
    private int maxJoinedWorkspaces = 10;
}
