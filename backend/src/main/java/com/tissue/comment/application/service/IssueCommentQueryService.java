package com.tissue.comment.application.service;

import com.tissue.comment.application.dto.response.CommentDetailResponse;
import com.tissue.comment.application.dto.response.MyCommentResponse;
import com.tissue.comment.application.port.in.CommentQueryUseCase;
import com.tissue.comment.application.port.out.CommentQueryRepository;
import com.tissue.comment.domain.Comment;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueCommentQueryService implements CommentQueryUseCase {

    private final CommentQueryRepository commentQueryRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public List<CommentDetailResponse> getIssueComments(String issueKey, ProjectMemberContext actor) {
        // TODO: To pass ProjectMemberContext from the controller, the actor is already checked at
        //  the ProjectMemberArgumentResolver. Do i really need to call requireProjectViewer?
        projectAuthorizationService.requireProjectViewer(actor);

        List<Comment> allComments = commentQueryRepository.findByIssue(actor.workspaceKey(), issueKey);

        Map<Long, List<Comment>> repliesByParentId = allComments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));

        return allComments.stream()
                .filter(c -> c.getParentComment() == null)
                .map(root -> {
                    List<Comment> replies = repliesByParentId.getOrDefault(root.getId(), List.of());
                    List<CommentDetailResponse> replyDtos = replies.stream()
                            .map(reply -> CommentDetailResponse.from(reply, List.of()))
                            .toList();
                    return CommentDetailResponse.from(root, replyDtos);
                })
                .toList();
    }

    @Override
    public Page<MyCommentResponse> getMyComments(Long memberId, Pageable pageable) {
        return commentQueryRepository.findByAuthor(memberId, pageable).map(MyCommentResponse::from);
    }
}
