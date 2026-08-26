package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.dto.response.ProjectHardDeletePreview;
import com.tissue.feature.project.application.port.repository.ProjectPurgeRepository;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.project.domain.exception.ProjectNotFoundException;
import com.tissue.global.filestorage.FileStorageClient;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Permanently deletes (hard-delete) an already soft-deleted project and every resource that hangs off it
 * (issue subtree, sprints, tags, project members, VCS integrations, activity log) + physical attachment files.
 * The project must already be soft-deleted.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "Passes AI written integration test, still needs review.",
        agentName = "claude-opus-4-8")
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectHardDeleteService {

    private final ProjectQueryRepository projectQueryRepository;
    private final ProjectPurgeRepository purgeRepository;
    private final FileStorageClient fileStorageClient;
    private final ProjectEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public ProjectHardDeletePreview preview(String projectKey) {
        Project project = ensureSoftDeleted(projectKey);
        return buildCounts(project);
    }

    public ProjectHardDeletePreview hardDelete(String projectKey) {
        Project project = ensureSoftDeleted(projectKey);
        Long projectId = project.getId();

        List<String> storedPaths = purgeRepository.findStoredPaths(projectId);
        ProjectHardDeletePreview counts = buildCounts(project);

        // Issue subtree
        purgeRepository.deleteIssueRelations(projectId);
        purgeRepository.deleteAttachments(projectId);
        purgeRepository.deleteIssueBranches(projectId);
        purgeRepository.deleteIssuePullRequests(projectId);
        purgeRepository.deleteIssueReviewers(projectId);
        purgeRepository.deleteIssueSubscribers(projectId);
        purgeRepository.deleteIssueTags(projectId);
        purgeRepository.deleteComments(projectId);
        purgeRepository.deleteDanglingWikiLinks(projectId);
        purgeRepository.nullifyParentIssueReferences(projectId);
        purgeRepository.deleteIssues(projectId);

        // Project direct children
        purgeRepository.deleteTags(projectId);
        purgeRepository.deleteSprints(projectId);
        purgeRepository.deleteProjectMembers(projectId);

        purgeRepository.deleteVcsIntegrations(projectKey);
        purgeRepository.deleteActivityLogs(projectKey);

        // The project itself
        purgeRepository.deleteProject(projectId);

        scheduleFileCleanup(storedPaths);
        eventPublisher.publishHardDeleted(projectKey);

        log.info("Hard-deleted project {} (issues={}, files={})", projectKey, counts.issues(), storedPaths.size());
        return counts;
    }

    private Project ensureSoftDeleted(String projectKey) {
        return projectQueryRepository.findDeletedByKey(projectKey).orElseGet(() -> {
            if (projectQueryRepository.existsByKey(projectKey)) {
                throw new ResourceConflictException(ProjectErrorCode.PROJECT_NOT_SOFT_DELETED);
            }
            throw new ProjectNotFoundException(projectKey);
        });
    }

    private ProjectHardDeletePreview buildCounts(Project project) {
        Long projectId = project.getId();
        String projectKey = project.getKey();
        long attachments = purgeRepository.countAttachments(projectId);
        return ProjectHardDeletePreview.builder()
                .projectKey(projectKey)
                .issues(purgeRepository.countIssues(projectId))
                .comments(purgeRepository.countComments(projectId))
                .attachments(attachments)
                .files(attachments)
                .sprints(purgeRepository.countSprints(projectId))
                .tags(purgeRepository.countTags(projectId))
                .members(purgeRepository.countMembers(projectId))
                .activityLogs(purgeRepository.countActivityLogs(projectKey))
                .vcsIntegrations(purgeRepository.countVcsIntegrations(projectKey))
                .build();
    }

    /**
     * Deletes the stored files only after the surrounding transaction commits. Deleting inside the
     * transaction would lose the files permanently if the transaction later rolled back. If no transaction
     * is active (should not happen via the normal entry points), the files are deleted immediately.
     */
    private void scheduleFileCleanup(List<String> storedPaths) {
        if (storedPaths.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteFiles(storedPaths);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFiles(storedPaths);
            }
        });
    }

    private void deleteFiles(List<String> storedPaths) {
        for (String storedPath : storedPaths) {
            fileStorageClient.delete(storedPath);
        }
        log.info("Deleted {} attachment file(s) after project hard-delete", storedPaths.size());
    }
}
