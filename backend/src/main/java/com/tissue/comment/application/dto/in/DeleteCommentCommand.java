package com.tissue.comment.application.dto.in;

import com.tissue.project.application.dto.ProjectMemberContext;

public record DeleteCommentCommand(String issueKey, Long commentId, ProjectMemberContext actor) {}
