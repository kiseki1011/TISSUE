package com.tissue.api.assignee.domain;

import java.time.LocalDateTime;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EqualsAndHashCode(of = "assigneeMemberId", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueAssignee extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ISSUE_ID", nullable = false)
	private Issue issue;

	@Column(name = "ASSIGNEE_ID", nullable = false)
	private Long assigneeMemberId;  // ID만 직접 저장

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ASSIGNEE_ID", insertable = false, updatable = false)
	private WorkspaceMember assignee;

	@Column(nullable = false)
	private LocalDateTime assignedAt;

	public IssueAssignee(Issue issue, WorkspaceMember assignee) {
		this.issue = issue;
		this.assignee = assignee;
		this.assigneeMemberId = assignee.getMember().getId();
		this.assignedAt = LocalDateTime.now();
	}
}
