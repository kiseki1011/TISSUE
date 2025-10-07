package com.tissue.api.notification.domain.model.vo;

import com.tissue.api.notification.domain.enums.ResourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityReference {

	// TODO: WorkspaceMember는 workspaceKey + memberId로 사용하도록 리팩토링
	private static final String WORKSPACES = "/workspaces/";
	private static final String ISSUES = "/issues/";
	private static final String COMMENTS = "/comments/";
	private static final String REVIEWS = "/reviews/";
	private static final String SPRINTS = "/sprints/";
	private static final String MEMBERS = "/members/";

	@Column(nullable = false)
	private String workspaceCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ResourceType resourceType;

	// 키 기반 식별자 (issueKey, sprintKey...)
	private String stringKey;

	// 숫자 식별자 (workspaceMemberId, rev)
	private Long primaryId;
	private Long secondaryId;
	private Long tertiaryId;

	@Builder
	private EntityReference(
		String workspaceCode,
		ResourceType resourceType,
		String stringKey,
		Long primaryId,
		Long secondaryId,
		Long tertiaryId
	) {
		this.workspaceCode = workspaceCode;
		this.resourceType = resourceType;
		this.stringKey = stringKey;
		this.primaryId = primaryId;
		this.secondaryId = secondaryId;
		this.tertiaryId = tertiaryId;
	}

	public static EntityReference forIssue(String workspaceCode, String issueKey) {
		return EntityReference.builder()
			.workspaceCode(workspaceCode)
			.resourceType(ResourceType.ISSUE)
			.stringKey(issueKey)
			.build();
	}

	public static EntityReference forIssueComment(
		String workspaceCode,
		String issueKey,
		Long commentId
	) {
		return EntityReference.builder()
			.workspaceCode(workspaceCode)
			.resourceType(ResourceType.ISSUE_COMMENT)
			.stringKey(issueKey)
			.primaryId(commentId)
			.build();
	}

	public static EntityReference forReview(
		String workspaceCode,
		String issueKey,
		Long reviewId
	) {
		return EntityReference.builder()
			.workspaceCode(workspaceCode)
			.resourceType(ResourceType.REVIEW)
			.stringKey(issueKey)
			.primaryId(reviewId)
			.build();
	}

	public static EntityReference forReviewComment(
		String workspaceCode,
		String issueKey,
		Long reviewId,
		Long commentId
	) {
		return EntityReference.builder()
			.workspaceCode(workspaceCode)
			.resourceType(ResourceType.REVIEW_COMMENT)
			.stringKey(issueKey)
			.primaryId(reviewId)
			.secondaryId(commentId)
			.build();
	}

	public static EntityReference forSprint(String workspaceCode, String sprintKey) {
		return EntityReference.builder()
			.workspaceCode(workspaceCode)
			.resourceType(ResourceType.SPRINT)
			.stringKey(sprintKey)
			.build();
	}

	public static EntityReference forWorkspace(String workspaceCode) {
		return EntityReference.builder()
			.workspaceCode(workspaceCode)
			.resourceType(ResourceType.WORKSPACE)
			.build();
	}

	public static EntityReference forWorkspaceMember(String workspaceCode, Long memberId) {
		return EntityReference.builder()
			.workspaceCode(workspaceCode)
			.resourceType(ResourceType.WORKSPACE_MEMBER)
			.primaryId(memberId)
			.build();
	}

	// URL 생성 메서드
	public String toUrl() {
		String base = WORKSPACES + workspaceCode;

		return switch (resourceType) {
			case ISSUE -> base + ISSUES + stringKey;
			case ISSUE_COMMENT -> base + ISSUES + stringKey + COMMENTS + primaryId;
			case REVIEW -> base + ISSUES + stringKey + REVIEWS + primaryId;
			case REVIEW_COMMENT -> base + ISSUES + stringKey + REVIEWS + primaryId + COMMENTS + secondaryId;
			case SPRINT -> base + SPRINTS + stringKey;
			case WORKSPACE_MEMBER -> base + MEMBERS + primaryId;
			case WORKSPACE -> base;
		};
	}
}
