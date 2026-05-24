package com.tissue.feature.projecttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateDetail;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateSummary;
import com.tissue.feature.projecttemplate.application.port.repository.ProjectTemplateRepository;
import com.tissue.feature.projecttemplate.application.service.ProjectTemplateQueryService;
import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import com.tissue.feature.projecttemplate.domain.config.TemplateConfig;
import com.tissue.feature.projecttemplate.domain.exception.ProjectTemplateNotFoundException;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceMemberNotFoundException;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectTemplateQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectTemplateQueryService sut;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectTemplateRepository projectTemplateRepository;

    private Member gildong;
    private Member bob;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        bob = memberRepository.save(Member.create("bob@tissue.com", "bob", "Bob"));

        workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Workspace", null));
        workspaceMemberRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));

        em.flush();
        em.clear();
    }

    private ProjectTemplate saveTemplate(String name) {
        Workspace managed = em.find(Workspace.class, workspace.getId());
        ProjectTemplate template =
                ProjectTemplate.create(managed, name, "desc for " + name, new TemplateConfig(List.of(), List.of()));
        return projectTemplateRepository.save(template);
    }

    @Nested
    @DisplayName("getWorkspaceTemplates")
    class GetWorkspaceTemplates {

        @Test
        @DisplayName("returns every template of the workspace")
        void returnsAllTemplates() {
            // given
            saveTemplate("Frontend");
            saveTemplate("Backend");
            em.flush();
            em.clear();

            // when
            Page<ProjectTemplateSummary> page =
                    sut.getWorkspaceTemplates("WORKSPACE", PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(ProjectTemplateSummary::name)
                    .containsExactlyInAnyOrder("Frontend", "Backend");
        }

        @Test
        @DisplayName("returns an empty page when the workspace has no templates")
        void returnsEmptyPageWhenNoTemplates() {
            // when
            Page<ProjectTemplateSummary> page =
                    sut.getWorkspaceTemplates("WORKSPACE", PageRequest.of(0, 10), gildong.getId());

            // then
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("rejects non workspace member")
        void rejectsNonWorkspaceMember() {
            // when & then
            assertThatThrownBy(() -> sut.getWorkspaceTemplates("WORKSPACE", PageRequest.of(0, 10), bob.getId()))
                    .isInstanceOf(WorkspaceMemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getProjectTemplateDetail")
    class GetProjectTemplateDetail {

        @Test
        @DisplayName("returns the template with its config payload")
        void returnsDetailWithConfig() {
            // given
            ProjectTemplate template = saveTemplate("Frontend");
            em.flush();
            em.clear();

            // when
            ProjectTemplateDetail detail = sut.getProjectTemplateDetail("WORKSPACE", template.getId(), gildong.getId());

            // then
            assertThat(detail.id()).isEqualTo(template.getId());
            assertThat(detail.workspaceKey()).isEqualTo("WORKSPACE");
            assertThat(detail.name()).isEqualTo("Frontend");
            assertThat(detail.description()).isEqualTo("desc for Frontend");
            assertThat(detail.createdAt()).isNotNull();
            assertThat(detail.config()).isNotNull();
            assertThat(detail.config().workflows()).isEmpty();
            assertThat(detail.config().issueTypes()).isEmpty();
        }

        @Test
        @DisplayName("rejects non workspace member")
        void rejectsNonWorkspaceMember() {
            // given
            ProjectTemplate template = saveTemplate("Frontend");
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> sut.getProjectTemplateDetail("WORKSPACE", template.getId(), bob.getId()))
                    .isInstanceOf(WorkspaceMemberNotFoundException.class);
        }

        @Test
        @DisplayName("throws when the template does not exist in the workspace")
        void throwsWhenNotFound() {
            // when & then
            assertThatThrownBy(() -> sut.getProjectTemplateDetail("WORKSPACE", 999L, gildong.getId()))
                    .isInstanceOf(ProjectTemplateNotFoundException.class);
        }
    }
}
