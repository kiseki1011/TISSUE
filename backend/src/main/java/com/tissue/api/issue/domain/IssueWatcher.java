package com.tissue.api.issue.domain;

import java.time.LocalDateTime;

import com.tissue.api.common.entity.BaseDateEntity;
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
@EqualsAndHashCode(of = "watcherMemberId", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueWatcher extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "WATCHER_ID", nullable = false)
	private Long watcherMemberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WATCHER_ID", insertable = false, updatable = false)
	private WorkspaceMember watcher;

	// Todo: BaseDateEntity로 기록하는데, 굳이 필요할까?
	@Column(nullable = false)
	private LocalDateTime watchedAt;

	public IssueWatcher(WorkspaceMember watcher) {
		this.watcher = watcher;
		this.watcherMemberId = watcher.getMember().getId();
		this.watchedAt = LocalDateTime.now();
	}
}
