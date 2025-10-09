package com.tissue.api.issue.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueReviewer extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ISSUE_ID", nullable = false)
	private Issue issue;

	@Column(name = "REVIEWER_MEMBER_ID")
	private Long reviewerMemberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REVIEWER_ID", insertable = false, updatable = false)
	private WorkspaceMember reviewer;

	public IssueReviewer(WorkspaceMember reviewer, Issue issue) {
		this.reviewer = reviewer;
		this.reviewerMemberId = reviewer.getMember().getId();
		this.issue = issue;
	}
}
