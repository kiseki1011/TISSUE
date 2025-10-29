package com.tissue.api.issue.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueReviewer;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class IssueQueryJpaAdapter implements IssueQueryRepository {

	private final EntityManager em;

	@Override
	public Optional<Issue> findWithBasicInfo(
		String workspaceKey,
		String issueKey
	) {
		String jpql = """
			    SELECT i
			    FROM Issue i
			    JOIN FETCH i.workspace w
			    JOIN FETCH i.issueType it
			    JOIN FETCH it.workflow
			    JOIN FETCH i.currentState
			    WHERE w.key = :workspaceKey AND i.key = :issueKey
			""";

		return em.createQuery(jpql, Issue.class)
			.setParameter("workspaceKey", workspaceKey)
			.setParameter("issueKey", issueKey)
			.getResultStream()
			.findFirst();
	}

	@Override
	public Optional<Issue> findWithDetail(
		String workspaceKey,
		String issueKey
	) {
		String jpql = """
			    SELECT DISTINCT i
			    FROM Issue i
			    JOIN FETCH i.workspace w
			    JOIN FETCH i.issueType it
			    JOIN FETCH i.currentState cs
			    LEFT JOIN FETCH i.participants.assignee a
			    LEFT JOIN FETCH a.member am
			    JOIN FETCH i.participants.reporter r
			    JOIN FETCH r.member rm
			    LEFT JOIN FETCH i.participants.reviewers rev
			    LEFT JOIN FETCH rev.reviewer revWm
			    LEFT JOIN FETCH revWm.member revM
			    WHERE w.key = :workspaceKey AND i.key = :issueKey
			""";

		return em.createQuery(jpql, Issue.class)
			.setParameter("workspaceKey", workspaceKey)
			.getResultStream()
			.findFirst();
	}

	private void fetchReviewers(Issue issue) {
		String jpql = """
			    SELECT r
			    FROM IssueReviewer r
			    JOIN FETCH r.reviewer wm
			    JOIN FETCH wm.member m
			    WHERE r.issue = :issue
			""";

		List<IssueReviewer> reviewers = em.createQuery(jpql, IssueReviewer.class)
			.setParameter("issue", issue)
			.getResultList();
	}
}
