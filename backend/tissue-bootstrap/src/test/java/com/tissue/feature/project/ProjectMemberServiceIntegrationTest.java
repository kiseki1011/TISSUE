package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.service.ProjectMemberService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.IntegrationTestSupport;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectMemberServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    private ProjectMemberQueryRepository projectMemberQueryRepository;

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

    @Nested
    @DisplayName("add members")
    class AddMembers {

        @Test
        @DisplayName("adds only new members and skips already existing members")
        void addsNewAndSkipsExisting() {
            // given
            Member newMember = memberCommandRepository.save(Member.create("new@tissue.com", "newuser", "HongGilDong"));

            Member existingMember =
                    memberCommandRepository.save(Member.create("existing@tissue.com", "existing", "KimChulSoo"));
            projectMemberCommandRepository.save(ProjectMember.create(project, existingMember));
            em.flush();

            ProjectIdentifier pid = ProjectIdentifier.ofProjectKey("PROJ");

            // when
            ProjectMembersResponse response = projectMemberService.addMembers(
                    pid, Set.of(newMember.getId(), existingMember.getId()), manager.getId());
            em.flush();
            em.clear();

            // then
            assertThat(response.memberIds()).containsExactly(newMember.getId());
            assertThat(response.totalSize()).isEqualTo(1);

            assertThat(projectMemberQueryRepository.findWithMemberByProjectKeyAndMemberId("PROJ", newMember.getId()))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("join")
    class Join {

        @Test
        @DisplayName("re-joining after leaving restores the soft-deleted membership")
        void rejoinRestoresSoftDeletedMembership() {
            // given - a member who joined then left (soft-deleted)
            Member member = memberCommandRepository.save(Member.create("rejoin@tissue.com", "rejoiner", "Lee Sunshin"));
            ProjectIdentifier pid = ProjectIdentifier.ofProjectKey("PROJ");

            projectMemberService.join(pid, member.getId());
            em.flush();
            projectMemberService.leave(pid, member.getId());
            em.flush();
            em.clear();

            assertThat(projectMemberQueryRepository.findWithMemberByProjectKeyAndMemberId("PROJ", member.getId()))
                    .isEmpty();

            // when - member re-joins
            projectMemberService.join(pid, member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(projectMemberQueryRepository.findWithMemberByProjectKeyAndMemberId("PROJ", member.getId()))
                    .isPresent();
        }
    }
}
