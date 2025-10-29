package com.tissue.api.issue.application.port.out;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.IssueSubscriber;

public interface IssueSubscriberQueryRepository extends Repository<IssueSubscriber, Long> {

	/**
	 * 특정 이슈의 구독자 목록 조회
	 */
	@Query("""
		    SELECT s
		    FROM IssueSubscriber s
		    JOIN FETCH s.subscriber wm
		    JOIN FETCH wm.member m
		    JOIN FETCH s.issue i
		    JOIN FETCH i.workspace w
		    WHERE w.key = :workspaceKey 
		      AND i.key = :issueKey
		""")
	List<IssueSubscriber> findByIssue(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	/**
	 * 구독자 수 조회
	 */
	@Query("""
		    SELECT COUNT(s)
		    FROM IssueSubscriber s
		    JOIN s.issue i
		    JOIN i.workspace w
		    WHERE w.key = :workspaceKey 
		      AND i.key = :issueKey
		""")
	int countByIssue(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	/**
	 * 특정 회원이 특정 이슈를 구독하는지 확인
	 */
	@Query("""
		    SELECT COUNT(s) > 0
		    FROM IssueSubscriber s
		    JOIN s.issue i
		    JOIN i.workspace w
		    WHERE w.key = :workspaceKey 
		      AND i.key = :issueKey
		      AND s.subscriber.member.id = :memberId
		""")
	boolean existsByIssueAndMember(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey,
		@Param("memberId") Long memberId
	);
}
