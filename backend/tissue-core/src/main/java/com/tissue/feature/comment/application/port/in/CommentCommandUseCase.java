package com.tissue.feature.comment.application.port.in;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.project.application.dto.ProjectMemberContext;

public interface CommentCommandUseCase {

    CommentCreateResponse create(String issueKey, CreateCommentCommand cmd, ProjectMemberContext actorContext);

    void update(String issueKey, Long commentId, String content, ProjectMemberContext actorContext);

    void delete(String issueKey, Long commentId, ProjectMemberContext actorContext);
}
