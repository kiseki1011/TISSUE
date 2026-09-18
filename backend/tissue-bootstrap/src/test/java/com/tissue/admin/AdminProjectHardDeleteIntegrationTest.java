package com.tissue.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.admin.application.dto.AdminAuditLogResponse;
import com.tissue.admin.application.service.AdminAuditQueryService;
import com.tissue.admin.application.service.AdminProjectService;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditTargetType;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectHardDeletePreview;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectNotFoundException;
import com.tissue.feature.sprint.application.port.repository.SprintCommandRepository;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.VcsWebhookDelivery;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.IntegrationTestSupport;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "Needs review",
        agentName = "claude-opus-4-8")
@Transactional
class AdminProjectHardDeleteIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminProjectService adminProjectService;

    @Autowired
    private AdminAuditQueryService adminAuditQueryService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    private SprintCommandRepository sprintCommandRepository;

    @Autowired
    private ActivityLogCommandRepository activityLogCommandRepository;

    private long nativeCount(String sql, String param, Object value) {
        return ((Number) em.createNativeQuery(sql).setParameter(param, value).getSingleResult()).longValue();
    }

    private Project givenSoftDeletedProjectWithChildren(String key) {
        Member member =
                memberCommandRepository.save(Member.create(key + "@tissue.com", key.toLowerCase(Locale.ROOT), key));
        Project project = projectCommandRepository.save(Project.create(key, key + " title", "desc"));
        projectMemberCommandRepository.save(ProjectMember.create(project, member));
        sprintCommandRepository.save(Sprint.create(project, "Sprint 1", "goal"));
        em.persist(ProjectVcsIntegration.create(VcsProvider.GITHUB, key, "webhook-secret"));
        em.persist(
                VcsWebhookDelivery.create(VcsProvider.GITHUB, key + "-delivery-1", key, "push", "{\"ref\":\"main\"}"));
        activityLogCommandRepository.save(ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.ISSUE_CREATED)
                .entityReference(EntityReference.forIssue(key, key + "-1"))
                .data(Map.of())
                .changes(Map.of())
                .actorMemberId(member.getId())
                .build());
        project.softDelete();
        projectCommandRepository.save(project);
        return project;
    }

    @Nested
    @DisplayName("preview")
    class Preview {

        @Test
        @DisplayName("counts the resources a hard-delete would remove")
        void countsResources() {
            // given
            givenSoftDeletedProjectWithChildren("PROJ");
            em.flush();
            em.clear();

            // when
            ProjectHardDeletePreview preview = adminProjectService.previewHardDelete("PROJ");

            // then
            assertThat(preview.projectKey()).isEqualTo("PROJ");
            assertThat(preview.members()).isEqualTo(1);
            assertThat(preview.sprints()).isEqualTo(1);
            assertThat(preview.vcsIntegrations()).isEqualTo(1);
            assertThat(preview.webhookDeliveries()).isEqualTo(1);
            assertThat(preview.activityLogs()).isEqualTo(1);
            assertThat(preview.issues()).isZero();
            assertThat(preview.comments()).isZero();
            assertThat(preview.attachments()).isZero();
        }

        @Test
        @DisplayName("rejects a live (not soft-deleted) project with 409")
        void rejectsLiveProject() {
            // given
            projectCommandRepository.save(Project.create("LIVE", "Live", "desc"));
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> adminProjectService.previewHardDelete("LIVE"))
                    .isInstanceOf(ResourceConflictException.class);
        }

        @Test
        @DisplayName("rejects an unknown project with 404")
        void rejectsUnknownProject() {
            assertThatThrownBy(() -> adminProjectService.previewHardDelete("NOPE"))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("hardDelete")
    class HardDelete {

        @Test
        @DisplayName("purges the project and all of its children, and writes an audit entry")
        void purgesEverything() {
            // given
            Member actor = memberCommandRepository.save(Member.createAsSuperAdmin("su@tissue.com", "su", "Su"));
            Project project = givenSoftDeletedProjectWithChildren("PROJ");
            Long projectId = project.getId();
            em.flush();
            em.clear();

            // when
            ProjectHardDeletePreview result = adminProjectService.hardDelete("PROJ", "PROJ", actor.getId());
            em.flush();
            em.clear();

            // then
            assertThat(result.members()).isEqualTo(1);
            assertThat(result.sprints()).isEqualTo(1);

            // then: every row is gone
            assertThat(nativeCount("SELECT COUNT(*) FROM project WHERE id = :id", "id", projectId))
                    .isZero();
            assertThat(nativeCount("SELECT COUNT(*) FROM project_member WHERE project_id = :id", "id", projectId))
                    .isZero();
            assertThat(nativeCount("SELECT COUNT(*) FROM sprint WHERE project_id = :id", "id", projectId))
                    .isZero();
            assertThat(nativeCount("SELECT COUNT(*) FROM activity_log WHERE project_key = :k", "k", "PROJ"))
                    .isZero();
            assertThat(nativeCount("SELECT COUNT(*) FROM project_vcs_integration WHERE project_key = :k", "k", "PROJ"))
                    .isZero();
            assertThat(nativeCount("SELECT COUNT(*) FROM vcs_webhook_delivery WHERE project_key = :k", "k", "PROJ"))
                    .isZero();

            // then: an audit entry was recorded
            Page<AdminAuditLogResponse> audit = adminAuditQueryService.listAuditLogs(
                    null, AdminAuditAction.HARD_DELETE_PROJECT, null, PageRequest.of(0, 20));
            assertThat(audit.getContent()).hasSize(1);
            AdminAuditLogResponse entry = audit.getContent().getFirst();
            assertThat(entry.targetType()).isEqualTo(AdminAuditTargetType.PROJECT);
            assertThat(entry.targetRef()).isEqualTo("PROJ");
            assertThat(entry.actorMemberId()).isEqualTo(actor.getId());
        }

        @Test
        @DisplayName("rejects a confirmation key that does not match the project key with 400")
        void rejectsConfirmationMismatch() {
            // given
            Member actor = memberCommandRepository.save(Member.createAsSuperAdmin("su@tissue.com", "su", "Su"));
            givenSoftDeletedProjectWithChildren("PROJ");
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> adminProjectService.hardDelete("PROJ", "WRONG", actor.getId()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("rejects a live (not soft-deleted) project with 409")
        void rejectsLiveProject() {
            // given
            Member actor = memberCommandRepository.save(Member.createAsSuperAdmin("su@tissue.com", "su", "Su"));
            projectCommandRepository.save(Project.create("LIVE", "Live", "desc"));
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> adminProjectService.hardDelete("LIVE", "LIVE", actor.getId()))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    /**
     * Denormalized tables the purge removes by project_key (no FK to cascade through). The
     * vcs_webhook_delivery omission that this guard exists to catch lived exactly here.
     */
    private static final java.util.Set<String> PURGED_BY_PROJECT_KEY =
            java.util.Set.of("activity_log", "project_vcs_integration", "vcs_webhook_delivery");

    /**
     * Tables that carry a project_key but are intentionally NOT removed by it: the aggregate root
     * (deleted by id last), children deleted by project_id, and user-scoped notification history.
     */
    private static final java.util.Set<String> EXEMPT_FROM_PROJECT_KEY_PURGE =
            java.util.Set.of("project", "project_member", "sprint", "tag", "notification");

    @Test
    @DisplayName("every project_key-scoped table is either purged or explicitly exempt")
    void everyProjectKeyScopedTableIsAccountedFor() {
        @SuppressWarnings("unchecked")
        java.util.List<String> tablesWithProjectKey = em.createNativeQuery(
                        "SELECT DISTINCT table_name FROM information_schema.columns "
                                + "WHERE table_schema = 'public' AND column_name = 'project_key'")
                .getResultList();

        java.util.Set<String> unaccounted = new java.util.HashSet<>(tablesWithProjectKey);
        unaccounted.removeAll(PURGED_BY_PROJECT_KEY);
        unaccounted.removeAll(EXEMPT_FROM_PROJECT_KEY_PURGE);

        assertThat(unaccounted)
                .as("a table carries project_key but is neither purged by the hard-delete cascade nor listed as "
                        + "exempt; decide whether a project hard-delete must remove it, then update "
                        + "ProjectPurgeRepository/ProjectHardDeleteService or the exemption list here")
                .isEmpty();
    }
}
