package com.tissue.vcs.application.service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.application.service.IssueTransitionService;
import com.tissue.issue.domain.Issue;
import com.tissue.vcs.application.port.in.GitProviderUseCase;
import com.tissue.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.vcs.domain.GitPrDto;
import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.vcs.domain.enums.PrAction;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.WorkspaceMember;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitIntegrationService implements GitProviderUseCase {

    private final IssueTransitionService issueTransitionService;
    private final WorkspaceVcsIntegrationRepository integrationRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    // issue key pattern: PROJ-123 (Strict word boundary)
    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b");

    @Override
    @Transactional
    public void handlePullRequest(GitPrDto gitPr) {
        log.info("[GIT_PR] action={}, workspace={}, title={}", gitPr.action(), gitPr.workspaceKey(), gitPr.title());

        // TODO: IllegalArgumentException vs WorkspaceVcsIntegrationNotFound 어느게 더 알맞지 이 상황에?
        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKey(gitPr.workspaceKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Integration not configured for workspace: " + gitPr.workspaceKey()));

        if (!integration.isGithubSyncEnabled()) {
            log.info("[GIT_PR] GitHub sync is disabled for workspace: {}", gitPr.workspaceKey());
            return;
        }

        String issueKey = extractIssueKey(gitPr.title());
        if (issueKey == null) {
            log.debug("No issue key found in PR title");
            return;
        }

        Issue issue = issueQueryRepository
                .findByKeyAndWorkspaceKey(issueKey, gitPr.workspaceKey())
                .orElseGet(() -> {
                    log.warn("[GIT_PR] Issue not found: {}", issueKey);
                    return null;
                });

        if (issue == null) {
            return;
        }

        findActor(gitPr.authorEmail(), gitPr.workspaceKey())
                .ifPresent(actor -> log.info(
                        "[GIT_PR] Identified matching actor username: {}, email: {}",
                        actor.getMember().getUsername(),
                        actor.getMember().getEmail()));

        // TODO: Add Activity Log - 이벤트 발행해서 ActivityLogEventListener에서 처리

        processWorkflowTransition(issue, gitPr);
    }

    @Nullable
    private String extractIssueKey(@Nullable String title) {
        if (title == null) {
            return null;
        }

        Matcher matcher = ISSUE_KEY_PATTERN.matcher(title);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    // TODO: 깃허브 유저와 actor 유저의 매칭 개선?
    //  현재 AuthIdentity에서 Member를 바라보는 단방향 연관관계가 있음.
    //  AuthIdentity의 경우 provider가 EMAIL이라면 identifier가 email이고, 다른 provider(GOOGLE, GITHUB)의 경우
    //  사용하는 identifier는 서로 다름.
    //  여기서 provider가 EMAIL이라면 identifier를 그냥 username으로 하고, AuthIdentity에는 email 필드를 따로
    //  두는게 좋지 않을까? 그럼 email을 통해 actor를 찾더라도 AuthProvider == GITHUB, AuthIdentity.getEmail == {githubPr.email}
    //  같은 식으로 찾으면 되고.
    //  그리고 Github로 소셜 로그인 및 회원가입을 했거나, 연동을 시켜놨지만, Member의 email은 Github 계정과 동일한 email이 아닌 경우도 있을 수 있잖아?
    //  이런건 어떻게 처리하는게 좋을까? 액터를 찾는걸 어떻게 설계하면 좋을지 모르겠네.
    private Optional<WorkspaceMember> findActor(@Nullable String email, String workspaceKey) {
        if (email == null) {
            return Optional.empty();
        }

        return workspaceMemberQueryRepository.findByMember_EmailAndWorkspaceKey(email, workspaceKey);
    }

    private void processWorkflowTransition(Issue issue, GitPrDto gitPr) {
        // TODO: 아래 체크 코드는 필요없을 것 같은데. 애초에 Issue의 currentState 또는
        //  Issue의 IssueType의 Workflow는 null 일수가 없음(제약으로 어차피 막힘). 있으면 버그 상태임.
        if (issue.getCurrentState() == null || issue.getIssueType().getWorkflow() == null) {
            return;
        }

        WorkflowTransition transition = null;
        switch (gitPr.action()) {
            case PrAction.OPENED, PrAction.REOPENED ->
                transition = issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrOpenedTransition();
            case MERGED ->
                transition = issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrMergedTransition();
            default -> log.debug("PR ignored");
        }

        if (transition == null) {
            return;
        }

        // validate if the current issue state is the source state of the transition
        if (currentStateNotMatchTransitionSourceState(issue, transition)) {
            log.info(
                    "Issue {} is in state {}, but VCS transition requires state {}. Skipping automation.",
                    issue.getKey(),
                    issue.getCurrentState().getName().getDisplay(),
                    transition.getSourceState().getName().getDisplay());
            return;
        }

        log.info(
                "[GIT_PR] Transitioning issue {} via VCS automation: {} -> {}",
                issue.getKey(),
                transition.getSourceState().getName().getDisplay(),
                transition.getTargetState().getName().getDisplay());

        issue.transitionTo(transition.getTargetState());

        // TODO: issue.transitionTo를 사용하기 보다는 performTransition()를 활용하는게 낫지 않나?
        //  왜냐하면 performTransition 안에 가드를 실행하는 로직이 들어가 있음
        //  문제는 ProjectMemberContext가 필요한데, 만약 깃허브 유저와 우리의 WorkspaceMember를 매칭하지
        //  못한 경우에는 actor에 해당하는 ProjectMemberContext를 만들어내지 못함.
        //  이런 경우를 우회하기 위한 performTransitionByVcs를 따로 만들어줘서 사용하는게 좋으려나?
        //  그리고 만약에 만드는게 좋다면 어디에 만들지? 이 클래스 안에 넣어서 응집성을 높여?
        //  아니면 기능 분리의 측면에서 issueTransitionService에 정의해서 호출해서 사용해?

        //        PerformTransitionCommand = new PerformTransitionCommand();
        //        issueTransitionService.performTransition();
    }

    private boolean currentStateNotMatchTransitionSourceState(Issue issue, WorkflowTransition transition) {
        return !Objects.equals(issue.getCurrentState(), transition.getSourceState());
    }
}
