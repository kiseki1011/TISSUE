package com.tissue.api.issue.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tissue.api.issue.application.dto.response.IssueDetail;
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
	// TODO: issue.getIssueType().getWorkflow();를 한번에 가져오기 위한 join fetch 필요
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
	// TODO: 이 방식(DTO에 프로젝션) 말고 그냥 Issue에 필요한 연관 엔티티들을 fetch join하고,
	//  IssueDetailResponse는 서비스 계층에서 조립해서 반환하는 방식은 어떨까?
	@Override
	public Optional<IssueDetail> findDetailedIssue(
		String workspaceKey,
		String issueKey
	) {
		String path = "com.tissue.api.issue.application.dto.response";
		String jpql = """
			    SELECT new %s.IssueDetailResponse(
			        i.id,
			        i.key,
			        i.title,
			        i.content.content,
			        i.content.summary,
			        i.priority,
			        i.storyPoint,
			        i.schedule.dueAt,
			        i.schedule.startedAt,
			        i.schedule.resolvedAt,
			        i.progress.countBasedProgress,
			        i.progress.pointBasedProgress,
			        
			        new %s.IssueDetailResponse.IssueTypeInfo(
			            it.id,
			            it.label.display
			        ),
			        
			        new %s.IssueDetailResponse.StateInfo(
			            ws.id,
			            ws.label.display,
			            ws.category
			        ),
			        
			        new %s.IssueDetailResponse.ParticipantInfo(
			            assigneeMember.id,
			            assigneeMember.username,
			            assignee.displayName
			        ),
			        
			        new %s.IssueDetailResponse.ParticipantInfo(
			            reporterMember.id,
			            reporterMember.username,
			            reporter.displayName
			        ),
			        
			        i.createdAt,
			        i.updatedAt
			    )
			    FROM Issue i
			    JOIN i.workspace w
			    JOIN i.issueType it
			    JOIN i.currentState ws
			    LEFT JOIN i.participants.assignee assignee
			    LEFT JOIN assignee.member assigneeMember
			    JOIN i.participants.reporter reporter
			    JOIN reporter.member reporterMember
			    WHERE w.key = :workspaceKey
			      AND i.key = :issueKey
			""".formatted(path, path, path, path, path);

		return em.createQuery(jpql, IssueDetail.class)
			.setParameter("workspaceKey", workspaceKey)
			.setParameter("issueKey", issueKey)
			.getResultStream()
			.findFirst();
	}
}
