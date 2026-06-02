package com.tissue.admin.application.service;

import com.tissue.admin.application.port.usecase.AdminProjectUseCase;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.feature.project.application.dto.response.ProjectHardDeletePreview;
import com.tissue.feature.project.application.service.ProjectHardDeleteService;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminProjectService implements AdminProjectUseCase {

    private final ProjectHardDeleteService projectHardDeleteService;
    private final AdminAuditRecorder adminAuditRecorder;

    @Override
    @Transactional(readOnly = true)
    public ProjectHardDeletePreview previewHardDelete(String projectKey) {
        return projectHardDeleteService.preview(projectKey);
    }

    @Override
    public ProjectHardDeletePreview hardDelete(String projectKey, String confirmationKey, Long actorMemberId) {
        ensureConfirmationKeyIdentical(projectKey, confirmationKey);

        ProjectHardDeletePreview result = projectHardDeleteService.hardDelete(projectKey);

        adminAuditRecorder.recordProjectAction(
                actorMemberId,
                AdminAuditAction.HARD_DELETE_PROJECT,
                projectKey,
                Map.of(
                        "issues", String.valueOf(result.issues()),
                        "files", String.valueOf(result.files()),
                        "members", String.valueOf(result.members())));

        return result;
    }

    private void ensureConfirmationKeyIdentical(String projectKey, String confirmationKey) {
        if (!projectKey.equals(confirmationKey)) {
            throw new BadRequestException(ProjectErrorCode.HARD_DELETE_CONFIRMATION_MISMATCH);
        }
    }
}
