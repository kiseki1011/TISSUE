package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.dto.AgentResponse;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.application.port.usecase.AgentUseCase;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.exception.AgentErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentService implements AgentUseCase {

    private static final int MAX_SLUG_LENGTH = 40;

    private final MemberFinder memberFinder;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberCommandRepository memberCommandRepository;

    @Override
    @Transactional
    public AgentResponse createAgent(Long ownerId, String name, @Nullable String declaredModel) {
        Member owner = memberFinder.getActiveById(ownerId);

        ensureOwnerIsHuman(owner);
        ensureAgentNameUniqueByOwner(ownerId, name);

        String username = generateUniqueUsername(owner.getUsername(), name);
        Member agent = memberCommandRepository.save(Member.createAgent(owner, username, name, declaredModel));

        return AgentResponse.from(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentResponse> listAgents(Long ownerId) {
        return memberQueryRepository.findAllByOwner_IdAndStatus(ownerId, MemberStatus.ACTIVE).stream()
                .map(AgentResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deactivateAgent(Long ownerId, Long agentId) {
        getOwnedActiveAgent(ownerId, agentId).withdraw();
    }

    @Transactional(readOnly = true)
    public Member getOwnedActiveAgent(Long ownerId, Long agentId) {
        Member agent = memberQueryRepository
                .findByIdAndOwner_Id(agentId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(AgentErrorCode.AGENT_NOT_FOUND));

        ensureAgentIsActive(agent);
        return agent;
    }

    @Transactional
    public List<Member> deactivateAllOwnedBy(Long ownerId) {
        List<Member> agents = memberQueryRepository.findAllByOwner_IdAndStatus(ownerId, MemberStatus.ACTIVE);
        agents.forEach(Member::withdraw);
        return agents;
    }

    private void ensureAgentNameUniqueByOwner(Long ownerId, String name) {
        if (memberQueryRepository.existsByOwner_IdAndNameAndStatus(ownerId, name, MemberStatus.ACTIVE)) {
            throw new ResourceConflictException(AgentErrorCode.DUPLICATE_AGENT_NAME);
        }
    }

    private void ensureOwnerIsHuman(Member owner) {
        if (!owner.isHuman()) {
            throw new ForbiddenException(AgentErrorCode.OWNER_MUST_BE_HUMAN);
        }
    }

    private void ensureAgentIsActive(Member agent) {
        if (!agent.isAgent() || !agent.isActive()) {
            throw new ResourceNotFoundException(AgentErrorCode.AGENT_NOT_FOUND);
        }
    }

    /**
     * Generates a globally unique handle of the form {@code agent-{ownerUsername}-{slug}}
     */
    private String generateUniqueUsername(String ownerUsername, String name) {
        String base = "agent-" + ownerUsername + "-" + slugify(name);
        String candidate = base;
        int suffix = 2;
        while (memberQueryRepository.existsByUsername(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private static String slugify(String name) {
        String slug = name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) {
            slug = "agent";
        }
        return slug.length() > MAX_SLUG_LENGTH ? slug.substring(0, MAX_SLUG_LENGTH) : slug;
    }
}
