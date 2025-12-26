package com.tissue.comment.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.comment.domain.Comment;

public interface CommentRepository extends Repository<Comment, Long> {
}
