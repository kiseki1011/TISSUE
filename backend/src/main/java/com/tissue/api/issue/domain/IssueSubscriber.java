package com.tissue.api.issue.domain;

import java.time.LocalDateTime;

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
public class IssueSubscriber extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// TODO: subscriber.getMember().getId()으로 탐색해서 저장하는데, 이렇게 하는건 별로일까?
	@Column(nullable = false)
	private Long subcriberMemberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "issue_id", nullable = false)
	private Issue issue;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(insertable = false, updatable = false)
	private WorkspaceMember subscriber;

	@Column(nullable = false)
	private LocalDateTime subscribedAt;

	public IssueSubscriber(WorkspaceMember subscriber, Issue issue) {
		this.issue = issue;
		this.subscriber = subscriber;
		this.subcriberMemberId = subscriber.getMember().getId();
		this.subscribedAt = LocalDateTime.now();
	}
}
