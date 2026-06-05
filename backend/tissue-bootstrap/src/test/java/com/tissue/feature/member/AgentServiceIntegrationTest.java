package com.tissue.feature.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.dto.AgentResponse;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.application.service.AgentService;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AgentServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AgentService agentService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    private Member newOwner(String username) {
        return memberCommandRepository.save(Member.create(username + "@tissue.dev", username, "Owner"));
    }

    @Test
    @DisplayName("success: creating an agent saves the AGENT owned by the actor with a generated handle (username)")
    void createPersistsAgent() {
        // given
        Member owner = newOwner("owner");

        // when
        AgentResponse response = agentService.createAgent(owner.getId(), "Code Reviewer", "claude-opus-99-9");

        // then
        assertThat(response.name()).isEqualTo("Code Reviewer");
        assertThat(response.username()).isEqualTo("agent-" + owner.getUsername() + "-code-reviewer");
        assertThat(response.declaredModel()).isEqualTo("claude-opus-99-9");

        Member agent = memberQueryRepository.findById(response.id()).orElseThrow();
        assertThat(agent.isAgent()).isTrue();
        assertThat(agent.getRole()).isEqualTo(SystemRole.USER);
        assertThat(agent.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(agent.getEmail()).isNull();
        assertThat(memberQueryRepository.findAllByOwner_IdAndStatus(owner.getId(), MemberStatus.ACTIVE))
                .extracting(Member::getId)
                .containsExactly(response.id());
    }

    @Test
    @DisplayName("fail: a duplicate agent name for the same owner is rejected")
    void rejectsDuplicateNameForSameOwner() {
        // given
        Member owner = newOwner("owner");
        agentService.createAgent(owner.getId(), "Bot", null);

        // when & then
        assertThatThrownBy(() -> agentService.createAgent(owner.getId(), "Bot", null))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("success: two owners may each have an agent with the same name")
    void allowsSameNameAcrossOwners() {
        // given
        Member first = newOwner("gildong");
        Member second = newOwner("alice");

        // when
        AgentResponse a = agentService.createAgent(first.getId(), "Bot", null);
        AgentResponse b = agentService.createAgent(second.getId(), "Bot", null);

        // then
        assertThat(a.username()).isEqualTo("agent-gildong-bot");
        assertThat(b.username()).isEqualTo("agent-alice-bot");
    }

    @Test
    @DisplayName("success: listing returns only the actor's active agents")
    void listsOnlyOwnActiveAgents() {
        // given
        Member owner = newOwner("owner");
        Member other = newOwner("other");
        AgentResponse kept = agentService.createAgent(owner.getId(), "Keep", null);
        AgentResponse dropped = agentService.createAgent(owner.getId(), "Drop", null);
        agentService.createAgent(other.getId(), "Theirs", null);
        agentService.deactivateAgent(owner.getId(), dropped.id());

        // when & then
        assertThat(agentService.listAgents(owner.getId()))
                .extracting(AgentResponse::id)
                .containsExactly(kept.id());
    }

    @Test
    @DisplayName("success: deactivating an agent withdraws it")
    void deactivateWithdrawsAgent() {
        // given
        Member owner = newOwner("owner");
        AgentResponse agent = agentService.createAgent(owner.getId(), "Bot", null);

        // when
        agentService.deactivateAgent(owner.getId(), agent.id());

        // then
        assertThat(memberQueryRepository.findById(agent.id()).orElseThrow().isDeleted())
                .isTrue();
    }

    @Test
    @DisplayName("fail: deactivating an agent you do not own throws not found exception")
    void cannotDeactivateOthersAgent() {
        // given
        Member owner = newOwner("owner");
        Member intruder = newOwner("intruder");
        AgentResponse agent = agentService.createAgent(owner.getId(), "Bot", null);

        // when & then
        assertThatThrownBy(() -> agentService.deactivateAgent(intruder.getId(), agent.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
