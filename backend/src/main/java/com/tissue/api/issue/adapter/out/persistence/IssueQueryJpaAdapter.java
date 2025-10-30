package com.tissue.api.issue.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;

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
			    SELECT i
			    FROM Issue i
			    JOIN FETCH i.workspace w
			    JOIN FETCH i.issueType it
			    JOIN FETCH i.currentState cs
			    LEFT JOIN FETCH i.participants.assignee a
			    LEFT JOIN FETCH a.member am
			    JOIN FETCH i.participants.reporter r
			    JOIN FETCH r.member rm
			    WHERE w.key = :workspaceKey AND i.key = :issueKey
			""";

		return em.createQuery(jpql, Issue.class)
			.setParameter("workspaceKey", workspaceKey)
			.setParameter("issueKey", issueKey)
			.getResultStream()
			.findFirst();
	}

	// TODO: findWithRelations를 추가해서 사용할까?

	@Override
	public Optional<Issue> findWithParent(String workspaceKey, String issueKey) {
		String jpql = """
			    SELECT i
			    FROM Issue i
			    JOIN FETCH i.workspace w
			    LEFT JOIN FETCH i.parent p
			    LEFT JOIN FETCH p.issueType pit
			    WHERE w.key = :workspaceKey AND i.key = :issueKey
			""";

		return em.createQuery(jpql, Issue.class)
			.setParameter("workspaceKey", workspaceKey)
			.setParameter("issueKey", issueKey)
			.getResultStream()
			.findFirst();
	}

	@Override
	public List<Issue> findChildren(String workspaceKey, String issueKey) {
		String jpql = """
			    SELECT child
			    FROM Issue child
			    JOIN FETCH child.issueType it
			    JOIN child.parent parent
			    JOIN parent.workspace w
			    WHERE w.key = :workspaceKey AND parent.key = :issueKey
			    ORDER BY child.createdAt ASC
			""";

		return em.createQuery(jpql, Issue.class)
			.setParameter("workspaceKey", workspaceKey)
			.setParameter("issueKey", issueKey)
			.getResultList();
	}
}
