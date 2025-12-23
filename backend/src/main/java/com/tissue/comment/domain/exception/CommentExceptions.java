package com.tissue.comment.domain.exception;

import static com.tissue.comment.domain.exception.CommentErrorCode.*;
import static com.tissue.global.exception.ContextKeys.*;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.global.exception.base.ResourceNotFoundException;

public class CommentExceptions {

	private CommentExceptions() {
	}

	public static ResourceNotFoundException notFound(Long commentId) {
		return new ResourceNotFoundException(COMMENT_NOT_FOUND)
			.addContext("commentId", commentId);
	}

	public static ForbiddenException notAuthor(Long commentId, Long memberId) {
		return new ForbiddenException(NOT_COMMENT_AUTHOR)
			.addContext("commentId", commentId)
			.addContext(MEMBER_ID, memberId);
	}

	public static BadRequestException nestedLimitExceeded(Long parentId) {
		return new BadRequestException(NESTED_COMMENT_LIMIT_EXCEEDED)
			.addContext("parentId", parentId);
	}

	public static BadRequestException relationMismatch(Long commentId, String expectedRelation) {
		return new BadRequestException(COMMENT_RELATION_MISMATCH)
			.addContext("commentId", commentId)
			.addContext("expectedRelation", expectedRelation);
	}
}
