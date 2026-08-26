package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.issue.application.dto.response.info.CustomFieldValueInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated issue detail view.")
public record IssueDetailView(
        IssueCommonDetail common,
        List<CustomFieldValueInfo> customFields,
        List<TransitionDetail> availableTransitions,
        IssueIdentifierResponse parent,
        List<IssueIdentifierResponse> children,
        IssueRelationsDetail relations,
        PageResponse<CommentDetailResponse> comments,
        List<IssueBranchView> branches,
        List<IssuePullRequestView> pullRequests) {}
