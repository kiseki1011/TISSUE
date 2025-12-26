package com.tissue.comment.application.port.in;

import static com.tissue.security.authorization.project.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.comment.application.dto.in.AddCommentCommand;
import com.tissue.comment.application.dto.in.DeleteCommentCommand;
import com.tissue.comment.application.dto.in.UpdateCommentCommand;
import com.tissue.comment.application.dto.out.CommentAddResponse;

public interface CommentCommandUseCase {

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	CommentAddResponse add(AddCommentCommand cmd);

	void update(UpdateCommentCommand cmd);

	void delete(DeleteCommentCommand cmd);
}
