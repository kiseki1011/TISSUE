package com.tissue.issuetype.application.dto.request;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateIssueFieldCommand(
    Long issueTypeId,
    Name name,
    @Nullable String description,
    IssueFieldType issueFieldType,
    Boolean required,
    List<Name> initialOptions,
    ProjectMemberContext actorContext) {

}
