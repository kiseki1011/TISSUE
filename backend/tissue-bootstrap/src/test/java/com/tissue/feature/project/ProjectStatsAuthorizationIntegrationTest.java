package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectStatsQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectVisibility;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * The statistics authorization policy: simple-stats is a browse preview (PUBLIC visible to any member,
 * PRIVATE member-only), while every deep stat is member-only regardless of visibility.
 */
@Transactional
class ProjectStatsAuthorizationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectStatsQueryService sut;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private Member member;
    private Member outsider;
    private Project publicProject;
    private Project privateProject;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("member@tissue.com", "member", "Member"));
        outsider = memberRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));

        publicProject = projectRepository.save(Project.create("PUB", "Public", null));
        Project priv = Project.create("PRIV", "Private", null);
        priv.updateVisibility(ProjectVisibility.PRIVATE);
        privateProject = projectRepository.save(priv);

        projectMemberRepository.save(ProjectMember.create(publicProject, member));
        projectMemberRepository.save(ProjectMember.create(privateProject, member));
        em.flush();
    }

    @Test
    @DisplayName("simple-stats: a non-member can preview a PUBLIC project")
    void simpleStatsPublicVisibleToNonMember() {
        assertThat(sut.getProjectSimpleStats(pid("PUB"), outsider.getId())).isNotNull();
    }

    @Test
    @DisplayName("simple-stats: a non-member cannot preview a PRIVATE project")
    void simpleStatsPrivateHiddenFromNonMember() {
        assertThatThrownBy(() -> sut.getProjectSimpleStats(pid("PRIV"), outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("simple-stats: a member can view a PRIVATE project")
    void simpleStatsPrivateVisibleToMember() {
        assertThat(sut.getProjectSimpleStats(pid("PRIV"), member.getId())).isNotNull();
    }

    @Test
    @DisplayName("aging stats are member-only even for a PUBLIC project")
    void agingStatsRejectsNonMemberOnPublicProject() {
        assertThatThrownBy(() -> sut.getProjectAgingStats(pid("PUB"), outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("member stats reject a non-member")
    void memberStatsRejectNonMember() {
        assertThatThrownBy(() -> sut.getProjectMemberStats(pid("PUB"), outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("flow stats reject a non-member")
    void flowStatsRejectNonMember() {
        assertThatThrownBy(() -> sut.getProjectFlowStats(pid("PUB"), "month", null, null, outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("cycle-time stats reject a non-member")
    void cycleTimeStatsRejectNonMember() {
        assertThatThrownBy(() -> sut.getProjectCycleTimeStats(pid("PUB"), "month", null, outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("sprint report rejects a non-member before it even looks up the sprint")
    void sprintReportRejectsNonMember() {
        assertThatThrownBy(() -> sut.getProjectSprintReport(pid("PUB"), 999L, outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("a member passes the deep-stats gate")
    void deepStatsAllowMember() {
        assertThat(sut.getProjectAgingStats(pid("PUB"), member.getId())).isNotNull();
    }

    private ProjectIdentifier pid(String key) {
        return ProjectIdentifier.ofProjectKey(key);
    }
}
