package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.response.ProjectMemberSummary;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.dto.ProjectIdentifier;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectMemberQueryUseCase {

    Page<ProjectMemberSummary> getProjectMembers(
            ProjectIdentifier pid,
            @Nullable ProjectRole role,
            @Nullable String keyword,
            Pageable pageable,
            Long actorMemberId);
}
