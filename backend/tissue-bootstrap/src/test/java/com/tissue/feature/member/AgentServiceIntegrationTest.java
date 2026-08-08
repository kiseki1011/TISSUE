package com.tissue.feature.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.agent.application.dto.AgentResponse;
import com.tissue.feature.agent.application.dto.CreateAgentCommand;
import com.tissue.feature.agent.application.dto.PatchAgentCommand;
import com.tissue.feature.agent.application.service.AgentService;
import com.tissue.feature.agent.domain.AgentType;
import com.tissue.feature.agent.model.application.port.repository.AiModelRepository;
import com.tissue.feature.agent.model.domain.AiModel;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;

class AgentServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AgentService agentService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    private Member newOwner(String username) {
        return memberCommandRepository.save(Member.create(username + "@tissue.dev", username, "Owner"));
    }

    private AiModel newModel(String name) {
        return aiModelRepository.save(AiModel.create(Name.of(name), "", ColorType.ANSI_BLUE));
    }

    private CreateAgentCommand createCmd(String name) {
        return CreateAgentCommand.builder().name(name).build();
    }

    @Test
    @DisplayName("success: creating an agent saves the AGENT owned by the actor with a generated handle (username)")
    void createPersistsAgent() {
        // given
        Member owner = newOwner("owner");

        // when
        AgentResponse response = agentService.createAgent(owner.getId(), createCmd("Code Reviewer"));

        // then
        assertThat(response.name()).isEqualTo("Code Reviewer");
        assertThat(response.username()).isEqualTo("agent-" + owner.getUsername() + "-code-reviewer");
        assertThat(response.agentType()).isEqualTo(AgentType.GENERAL);
        assertThat(response.model()).isNull();

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
    @DisplayName("success: creating an agent with a type, model, and description persists all three")
    void createPersistsTypeModelDescription() {
        // given
        Member owner = newOwner("owner");
        AiModel model = newModel("claude-opus-99-9");

        // when
        AgentResponse response = agentService.createAgent(
                owner.getId(),
                CreateAgentCommand.builder()
                        .name("Code Reviewer")
                        .agentType(AgentType.DEVELOPMENT)
                        .modelId(model.getId())
                        .description("Reviews pull requests")
                        .build());

        // then
        assertThat(response.agentType()).isEqualTo(AgentType.DEVELOPMENT);
        assertThat(response.model()).isNotNull();
        assertThat(response.model().id()).isEqualTo(model.getId());
        assertThat(response.model().name()).isEqualTo("claude-opus-99-9");
        assertThat(response.description()).isEqualTo("Reviews pull requests");
    }

    @Test
    @DisplayName("success: updating an agent patches its type, model, and description")
    void updatePatchesFields() {
        // given
        Member owner = newOwner("owner");
        AiModel model = newModel("gpt-9");
        AgentResponse agent = agentService.createAgent(owner.getId(), createCmd("Bot"));

        // when
        agentService.updateAgent(
                owner.getId(),
                agent.id(),
                PatchAgentCommand.builder()
                        .agentType(JsonNullable.of(AgentType.QA))
                        .modelId(JsonNullable.of(model.getId()))
                        .description(JsonNullable.of("Runs the test suite"))
                        .build());

        // then
        Member updated = memberQueryRepository.findById(agent.id()).orElseThrow();
        assertThat(updated.getAgentType()).isEqualTo(AgentType.QA);
        assertThat(updated.getModel()).isNotNull();
        assertThat(updated.getModel().getId()).isEqualTo(model.getId());
        assertThat(updated.getDescription()).isEqualTo("Runs the test suite");
    }

    @Test
    @DisplayName("fail: a duplicate agent name for the same owner is rejected")
    void rejectsDuplicateNameForSameOwner() {
        // given
        Member owner = newOwner("owner");
        agentService.createAgent(owner.getId(), createCmd("Bot"));

        // when & then
        assertThatThrownBy(() -> agentService.createAgent(owner.getId(), createCmd("Bot")))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("success: two owners may each have an agent with the same name")
    void allowsSameNameAcrossOwners() {
        // given
        Member first = newOwner("gildong");
        Member second = newOwner("alice");

        // when
        AgentResponse a = agentService.createAgent(first.getId(), createCmd("Bot"));
        AgentResponse b = agentService.createAgent(second.getId(), createCmd("Bot"));

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
        AgentResponse kept = agentService.createAgent(owner.getId(), createCmd("Keep"));
        AgentResponse dropped = agentService.createAgent(owner.getId(), createCmd("Drop"));
        agentService.createAgent(other.getId(), createCmd("Theirs"));
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
        AgentResponse agent = agentService.createAgent(owner.getId(), createCmd("Bot"));

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
        AgentResponse agent = agentService.createAgent(owner.getId(), createCmd("Bot"));

        // when & then
        assertThatThrownBy(() -> agentService.deactivateAgent(intruder.getId(), agent.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
