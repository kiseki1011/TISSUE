package com.tissue.issue.application.service.finder;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.enums.StateCategory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueQueryRepository issueQueryRepo;

	// TODO: 여기에 정의 x, 이벤트 리스너 쪽에서 사용 중인데, 거기서 id가 유효하지 않은 상황은 버그 상황에 가까움
	//  그래서 IllegalStateException을 사용하는게 옳다고 봄. (결론: 거기서 이슈 쿼리 레포에 바로 의존해서 조회하도록 한다
	//  그리고 던지는 예외는 스프링 기본 예외 사용)
	public Issue findBy(Long id) {
		return issueQueryRepo.findById(id)
			.orElseThrow(() -> new RuntimeException("Issue not found"));
	}

	public Issue findBy(String issueKey, Project project) {
		return issueQueryRepo.findByKeyAndProject(issueKey, project)
			.orElseThrow(() -> IssueExceptions.notFound(project.getWorkspaceKey(), issueKey));
	}

	public List<Issue> findAllBy(Collection<String> issueKeys, String workspaceKey) {
		return issueQueryRepo.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
	}

	public List<Issue> findIncompleteIssuesBySprint(Sprint sprint) {
		return issueQueryRepo.findIncompleteIssuesBySprint(sprint, StateCategory.DONE);
	}

	public List<String> findIncompleteIssueKeysBySprint(Sprint sprint) {
		return issueQueryRepo.findIncompleteIssueKeysBySprint(sprint, StateCategory.DONE);
	}
}
