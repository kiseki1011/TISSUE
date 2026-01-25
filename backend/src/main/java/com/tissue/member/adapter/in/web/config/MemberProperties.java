package com.tissue.member.adapter.in.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.member.policy")
public class MemberProperties {

    private boolean allowSignup = true;

    private int maxOwnedWorkspaces = 10;
    private int maxJoinedWorkspaces = 10;
}
