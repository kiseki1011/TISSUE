package com.tissue.comment.application.port.in;

import com.tissue.comment.application.dto.in.AddCommentCommand;
import com.tissue.comment.application.dto.in.DeleteCommentCommand;
import com.tissue.comment.application.dto.in.UpdateCommentCommand;
import com.tissue.comment.application.dto.out.CommentAddResponse;

public interface CommentCommandUseCase {

    CommentAddResponse add(AddCommentCommand cmd);

    void update(UpdateCommentCommand cmd);

    void delete(DeleteCommentCommand cmd);
}
