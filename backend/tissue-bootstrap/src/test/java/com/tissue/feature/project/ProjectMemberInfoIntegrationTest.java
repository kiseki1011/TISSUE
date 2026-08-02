package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectMemberInfoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectQueryService sut;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private Member gildong;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        em.flush();
    }

    @Test
    @DisplayName("counts the active members of a project")
    void countsActiveMembers() {
        // given
        Project apple = createProject("APPLE");
        projectMemberRepository.save(ProjectMember.create(apple, saveMember("a@tissue.com", "amember")));
        projectMemberRepository.save(ProjectMember.create(apple, saveMember("b@tissue.com", "bmember")));
        em.flush();

        // when
        long memberCount = summaryOf("APPLE").memberCount();

        // then
        assertThat(memberCount).isEqualTo(2);
    }

    @Test
    @DisplayName("excludes soft deleted members from the count")
    void excludesSoftDeletedMembers() {
        // given
        Project apple = createProject("APPLE");
        projectMemberRepository.save(ProjectMember.create(apple, saveMember("a@tissue.com", "amember")));
        ProjectMember removed = ProjectMember.create(apple, saveMember("b@tissue.com", "bmember"));
        removed.softDelete();
        projectMemberRepository.save(removed);
        em.flush();

        // when
        long memberCount = summaryOf("APPLE").memberCount();

        // then
        assertThat(memberCount).isEqualTo(1);
    }

    @Test
    @DisplayName("returns the caller's role as MEMBER")
    void returnsCallerRoleMember() {
        // given
        Project apple = createProject("APPLE");
        projectMemberRepository.save(ProjectMember.create(apple, gildong));
        em.flush();

        // when // then
        assertThat(summaryOf("APPLE").myRole()).isEqualTo(ProjectRole.MEMBER);
    }

    @Test
    @DisplayName("returns the caller's role as MANAGER")
    void returnsCallerRoleManager() {
        // given
        Project apple = createProject("APPLE");
        projectMemberRepository.save(ProjectMember.createManager(apple, gildong));
        em.flush();

        // when // then
        assertThat(summaryOf("APPLE").myRole()).isEqualTo(ProjectRole.MANAGER);
    }

    @Test
    @DisplayName("returns a null role when the caller is not a member")
    void returnsNullRoleForNonMember() {
        // given
        createProject("APPLE");

        // when // then
        assertThat(summaryOf("APPLE").myRole()).isNull();
    }

    private Project createProject(String key) {
        Project project = projectRepository.save(Project.create(key, key, null));
        em.flush();
        return project;
    }

    private Member saveMember(String email, String username) {
        Member member = memberRepository.save(Member.create(email, username, username));
        em.flush();
        return member;
    }

    private ProjectSummary summaryOf(String key) {
        Page<ProjectSummary> page = sut.getProjects(false, null, PageRequest.of(0, 50), gildong.getId());
        return page.getContent().stream()
                .filter(summary -> summary.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
