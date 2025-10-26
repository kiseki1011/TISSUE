package com.tissue.api.issue.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tissue.api.issue.application.dto.response.IssueDetailDto;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

// TODO: IssueQueryFinder보다 좋은 이름 없나?
@Repository
@RequiredArgsConstructor
public class IssueQueryJpaAdapter implements IssueQueryRepository {

	private final EntityManager em;

	// TODO: 틀린거 있는지 체크 필요
	@Override
	public Optional<Issue> findIssue(
		String workspaceKey,
		String issueKey
	) {
		String jpql = """
				SELECT *
				FROM Issue i JOIN i.workspace w
				WHERE w.key = :workspaceKey AND i.key = :issueKey
			""";

		return em.createQuery(jpql, Issue.class)
			.setParameter("workspaceKey", workspaceKey)
			.setParameter("issueKey", issueKey)
			.getResultStream()
			.findFirst();
	}

	// TODO: 틀린거 있는지 체크 필요
	@Override
	public Optional<IssueDetailDto> findDetailedIssue(
		String workspaceKey,
		String issueKey
	) {
		String jpql = """
			    SELECT new com.tissue.api.issue.application.dto.response.DetailedIssueDto(
			        i.id,
			        i.key,
			        i.title,
			        i.content,
			        i.priority,
			        i.storyPoint,
			        it.id, it.name, it.icon,
			        ws.id, ws.name, ws.category,
			        assigneeMember.id, assigneeMember.username, assigneeMember.displayName,
			        reporterMember.id, reporterMember.username, reporterMember.displayName,
			        creator.id
			        i.createdAt,
			        i.updatedAt
			    )
			    FROM Issue i
			    JOIN i.workspace w
			    JOIN i.issueType it
			    JOIN i.currentState ws
			    LEFT JOIN i.assignee assignee
			    LEFT JOIN assignee.member assigneeMember
			    LEFT JOIN i.participants.reporter reporter
			    LEFT JOIN reporter.member reporterMember
			    JOIN i.createdBy creator
			    WHERE w.key = :workspaceKey
			      AND i.key = :issueKey
			      AND i.deletedAt IS NULL
			""";

		return em.createQuery(jpql, IssueDetailDto.class)
			.setParameter("workspaceKey", workspaceKey)
			.setParameter("issueKey", issueKey)
			.getResultStream()
			.findFirst();
	}
}
