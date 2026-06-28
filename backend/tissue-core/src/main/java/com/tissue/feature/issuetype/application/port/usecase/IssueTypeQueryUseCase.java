package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeSummary;
import java.util.List;

public interface IssueTypeQueryUseCase {

    List<IssueTypeSummary> getIssueTypes(Long actorMemberId);

    IssueTypeDetail getIssueTypeDetail(Long issueTypeId, Long actorMemberId);

    List<IssueTypeDetail> getIssueTypeDetails(Long actorMemberId);
}
