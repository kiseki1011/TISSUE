package com.tissue.api.comment.domain;

import com.tissue.api.issue.domain.Issue;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("ISSUE_COMMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueComment {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ISSUE_ID", nullable = false)
	private Issue issue;
}
