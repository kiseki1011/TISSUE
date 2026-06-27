package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.MemberCandidateSummary;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectMemberQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectMemberCandidatesIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectMemberQueryService projectMemberQueryService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    private Member manager;
    private Project project;

    @BeforeEach
    void setUp() {
        manager = memberCommandRepository.save(Member.create("manager@tissue.com", "manager", "John Wick"));

        project = Project.create("PROJ", "Test Project", null);
        projectCommandRepository.save(project);
        projectMemberCommandRepository.save(ProjectMember.createManager(project, manager));
        em.flush();
    }

    private Page<MemberCandidateSummary> candidates(String keyword, Long actorId) {
        return projectMemberQueryService.getMemberCandidates(
                ProjectIdentifier.ofProjectKey("PROJ"), keyword, PageRequest.of(0, 20), actorId);
    }

    @Test
    @DisplayName("candidates match the keyword and exclude members already active in the project")
    void candidatesMatchKeywordAndExcludeActiveMembers() {
        // given: an outsider matching "ali" + an active member that must be excluded
        memberCommandRepository.save(Member.create("alice@tissue.com", "alice", "Alice Kim"));
        Member inProject = memberCommandRepository.save(Member.create("bob@tissue.com", "bob", "Alice Bob"));
        projectMemberCommandRepository.save(ProjectMember.create(project, inProject));
        em.flush();
        em.clear();

        // when: keyword "Alice" would match both by name, but bob is already a member
        Page<MemberCandidateSummary> result = candidates("Alice", manager.getId());

        // then
        assertThat(result.getContent())
                .extracting(MemberCandidateSummary::username)
                .containsExactly("alice");
    }

    @Test
    @DisplayName("an active member of the project is never a candidate")
    void activeMemberIsNotACandidate() {
        assertThat(candidates("manager", manager.getId()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("a removed (soft-deleted) member becomes a candidate again")
    void removedMemberIsCandidate() {
        // given: a member added then removed from the project
        Member removed = memberCommandRepository.save(Member.create("carol@tissue.com", "carol", "Carol Park"));
        ProjectMember membership = ProjectMember.create(project, removed);
        membership.softDelete();
        projectMemberCommandRepository.save(membership);
        em.flush();
        em.clear();

        assertThat(candidates("carol", manager.getId()).getContent())
                .extracting(MemberCandidateSummary::username)
                .containsExactly("carol");
    }

    @Test
    @DisplayName("matches on email too")
    void matchesOnEmail() {
        memberCommandRepository.save(Member.create("dave@example.com", "dave", "Dave"));
        em.flush();
        em.clear();

        assertThat(candidates("example.com", manager.getId()).getContent())
                .extracting(MemberCandidateSummary::username)
                .containsExactly("dave");
    }

    @Test
    @DisplayName("a blank keyword returns every member not active in the project")
    void blankKeywordReturnsAllCandidates() {
        memberCommandRepository.save(Member.create("e1@tissue.com", "ecand1", "E One"));
        memberCommandRepository.save(Member.create("e2@tissue.com", "ecand2", "E Two"));
        em.flush();
        em.clear();

        var usernames = candidates(null, manager.getId()).getContent().stream()
                .map(MemberCandidateSummary::username)
                .toList();
        assertThat(usernames).contains("ecand1", "ecand2").doesNotContain("manager");
    }

    @Test
    @DisplayName("a non-manager member is forbidden from searching candidates")
    void nonManagerForbidden() {
        Member plain = memberCommandRepository.save(Member.create("dan@tissue.com", "dan", "Dan"));
        projectMemberCommandRepository.save(ProjectMember.create(project, plain));
        em.flush();

        assertThatThrownBy(() -> candidates(null, plain.getId())).isInstanceOf(ForbiddenException.class);
    }
}
