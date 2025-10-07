package com.tissue.api.sprint.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.base.domain.model.Issue;

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
public class SprintIssue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SPRINT_ID", nullable = false)
	private Sprint sprint;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ISSUE_ID", nullable = false)
	private Issue issue;

	public SprintIssue(Sprint sprint, Issue issue) {
		this.sprint = sprint;
		this.issue = issue;
	}
}
