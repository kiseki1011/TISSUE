package com.tissue.comment.application.port.out;

import com.tissue.comment.domain.Comment;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface CommentRepository extends Repository<Comment, Long> {

    Comment save(Comment comment);

    Optional<Comment> findById(Long id);
}
