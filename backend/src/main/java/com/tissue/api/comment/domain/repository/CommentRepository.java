package com.tissue.api.comment.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.comment.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
