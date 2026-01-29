package com.tissue.comment.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record UpdateCommentCommand(String issueKey, Long commentId, String content, ProjectMemberContext actor) {}
