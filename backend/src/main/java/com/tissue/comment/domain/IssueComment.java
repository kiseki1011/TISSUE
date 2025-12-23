package com.tissue.comment.domain;

import com.tissue.issue.domain.Issue;
import com.tissue.workspace.domain.WorkspaceMember;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("ISSUE_COMMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueComment extends Comment {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "issue_id", nullable = false)
	private Issue issue;

	@Builder
	private IssueComment(
		String content,
		WorkspaceMember author,
		Issue issue,
		Comment parentComment
	) {
		super(content, author, parentComment);
		this.issue = issue;
	}
}
