package com.tissue.comment.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;

import com.tissue.comment.application.dto.in.AddCommentCommand;
import com.tissue.comment.application.dto.in.DeleteCommentCommand;
import com.tissue.comment.application.dto.in.UpdateCommentCommand;
import com.tissue.comment.application.dto.out.CommentAddResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CommentCommandUseCase {

    @PreAuthorize(REQUIRES_PROJECT_MEMBER)
    CommentAddResponse add(AddCommentCommand cmd);

    void update(UpdateCommentCommand cmd);

    void delete(DeleteCommentCommand cmd);
}
