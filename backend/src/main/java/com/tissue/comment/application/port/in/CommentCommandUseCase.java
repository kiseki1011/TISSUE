package com.tissue.comment.application.port.in;

import com.tissue.comment.application.dto.request.AddCommentCommand;
import com.tissue.comment.application.dto.request.DeleteCommentCommand;
import com.tissue.comment.application.dto.request.UpdateCommentCommand;
import com.tissue.comment.application.dto.response.CommentAddResponse;

public interface CommentCommandUseCase {

    CommentAddResponse add(AddCommentCommand cmd);

    void update(UpdateCommentCommand cmd);

    void delete(DeleteCommentCommand cmd);
}
