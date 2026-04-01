package com.tissue.feature.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.application.service.TagService;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.feature.tag.domain.exception.TagErrorCode;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TagServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private TagService tagService;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private static final ProjectIdentifier PID = new ProjectIdentifier("WORKSPACE", "PROJ");

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));
        Workspace workspace = workspaceRepository.save(Workspace.create(PID.workspaceKey(), "Test Workspace", null));
        Project project = projectRepository.save(Project.create(workspace, PID.projectKey(), "Test Project", null));
        WorkspaceMember workspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.createManager(project, workspaceMember));

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("create tag")
    class CreateTag {

        @Test
        @DisplayName("creates tag successfully")
        void successCreateTag() {
            // given
            CreateTagCommand cmd = CreateTagCommand.builder()
                    .name(Name.of("Bug"))
                    .description("Bug tag")
                    .color(ColorType.RED)
                    .build();

            // when
            TagResponse response = tagService.create(PID, cmd, member.getId());
            em.flush();
            em.clear();

            // then
            Tag tag = tagRepository
                    .findByWorkspaceKeyAndProjectKeyAndId(PID.workspaceKey(), PID.projectKey(), response.tagId())
                    .orElseThrow();

            assertThat(tag.getName().getDisplayName()).isEqualTo("Bug");
            assertThat(tag.getColor()).isEqualTo(ColorType.RED);
        }

        @Test
        @DisplayName("fails if tag name already exists in project")
        void failIfDuplicateName() {
            // given
            CreateTagCommand cmd = CreateTagCommand.builder()
                    .name(Name.of("Bug"))
                    .description(null)
                    .color(ColorType.RED)
                    .build();
            tagService.create(PID, cmd, member.getId());
            em.flush();

            CreateTagCommand duplicateCmd = CreateTagCommand.builder()
                    .name(Name.of("Bug"))
                    .description(null)
                    .color(ColorType.BLUE)
                    .build();

            // when & then
            assertThatThrownBy(() -> tagService.create(PID, duplicateCmd, member.getId()))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(TagErrorCode.DUPLICATE_TAG_NAME);
        }
    }

    @Nested
    @DisplayName("delete tag")
    class DeleteTag {

        @Test
        @DisplayName("hard deletes tag")
        void successDeleteTag() {
            // given
            CreateTagCommand cmd = CreateTagCommand.builder()
                    .name(Name.of("Urgent"))
                    .description(null)
                    .color(ColorType.GOLD)
                    .build();
            TagResponse response = tagService.create(PID, cmd, member.getId());
            em.flush();
            em.clear();

            // when
            tagService.delete(PID.workspaceKey(), response.tagId(), member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(tagRepository.findByWorkspaceKeyAndProjectKeyAndId(
                            PID.workspaceKey(), PID.projectKey(), response.tagId()))
                    .isEmpty();
        }
    }
}
