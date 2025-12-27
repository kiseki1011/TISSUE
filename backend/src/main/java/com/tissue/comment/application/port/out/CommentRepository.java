package com.tissue.comment.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.comment.domain.Comment;

public interface CommentRepository extends Repository<Comment, Long> {

	Comment save(Comment comment);

	Optional<Comment> findById(Long id);
}
