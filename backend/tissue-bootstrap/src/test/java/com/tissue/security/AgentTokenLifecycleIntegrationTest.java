package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.agent.application.dto.AgentResponse;
import com.tissue.feature.agent.application.dto.CreateAgentCommand;
import com.tissue.feature.agent.application.service.AgentService;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.GeneratedToken;
import com.tissue.security.application.service.MemberPurgeService;
import com.tissue.security.application.service.OwnedAgentDeactivationService;
import com.tissue.security.application.service.PersonalAccessTokenService;
import com.tissue.security.domain.PatScope;
import com.tissue.security.domain.PersonalAccessToken;
import com.tissue.support.IntegrationTestSupport;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AgentTokenLifecycleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AgentService agentService;

    @Autowired
    private PersonalAccessTokenService personalAccessTokenService;

    @Autowired
    private OwnedAgentDeactivationService ownedAgentDeactivationService;

    @Autowired
    private MemberPurgeService memberPurgeService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    private Member newOwner(String username) {
        return memberCommandRepository.save(Member.create(username + "@tissue.dev", username, "Owner"));
    }

    private GeneratedToken issueToken(AgentResponse agent, PatScope scope) {
        Member agentMember = memberQueryRepository.findById(agent.id()).orElseThrow();
        return personalAccessTokenService.generate(agentMember, "ci", scope, null);
    }

    @Test
    @DisplayName("success: an agent's token authenticates as that agent")
    void tokenAuthenticatesAsAgent() {
        // given
        Member owner = newOwner("owner");
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Mybot").build());
        GeneratedToken generated = issueToken(agent, PatScope.READ_ONLY);

        // when
        Optional<PersonalAccessToken> authenticated = personalAccessTokenService.authenticate(generated.rawToken());

        // then
        assertThat(authenticated).isPresent();
        assertThat(authenticated.get().getMember().getId()).isEqualTo(agent.id());
    }

    @Test
    @DisplayName("success: a deactivated agent's token that still exists, no longer authenticates")
    void deactivatedAgentTokenIsRejected() {
        // given
        Member owner = newOwner("owner");
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Mybot").build());
        GeneratedToken generated = issueToken(agent, PatScope.READ_WRITE);
        assertThat(personalAccessTokenService.authenticate(generated.rawToken()))
                .isPresent();

        // when
        agentService.deactivateAgent(owner.getId(), agent.id());

        // then
        assertThat(personalAccessTokenService.authenticate(generated.rawToken()))
                .isEmpty();
    }

    @Test
    @DisplayName("success: withdrawing the owner deactivates its agents and revokes their tokens")
    void ownerWithdrawalCascades() {
        // given
        Member owner = newOwner("owner");
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Mybot").build());
        GeneratedToken generated = issueToken(agent, PatScope.READ_WRITE);
        assertThat(personalAccessTokenService.authenticate(generated.rawToken()))
                .isPresent();

        // when
        ownedAgentDeactivationService.deactivateAgentsOf(owner.getId());

        // then
        assertThat(memberQueryRepository.findById(agent.id()).orElseThrow().isDeleted())
                .isTrue();
        assertThat(personalAccessTokenService.listFor(agent.id()))
                .singleElement()
                .extracting(PersonalAccessToken::isRevoked)
                .isEqualTo(true);
        assertThat(personalAccessTokenService.authenticate(generated.rawToken()))
                .isEmpty();
    }

    @Test
    @DisplayName("success: purging an deactivated agent removes its token rows entirely")
    void purgeRemovesAgentTokens() {
        // given
        Member owner = newOwner("owner");
        AgentResponse agent = agentService.createAgent(
                owner.getId(), CreateAgentCommand.builder().name("Mybot").build());
        issueToken(agent, PatScope.READ_ONLY);
        agentService.deactivateAgent(owner.getId(), agent.id());
        assertThat(personalAccessTokenService.listFor(agent.id())).hasSize(1);

        // when
        Member agentMember = memberQueryRepository.findById(agent.id()).orElseThrow();
        memberPurgeService.purge(agentMember);

        // then
        assertThat(personalAccessTokenService.listFor(agent.id())).isEmpty();
    }
}
