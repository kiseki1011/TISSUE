package com.tissue.feature.tag.application.service;

import com.tissue.feature.issue.application.port.repository.IssueTagRepository;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.feature.tag.application.dto.request.UpdateTagCommand;
import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.application.port.usecase.TagUseCase;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TagService implements TagUseCase {

    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectFinder projectFinder;
    private final TagFinder tagFinder;
    private final TagRepository tagRepository;
    private final TagValidator tagValidator;
    private final IssueTagRepository issueTagRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public TagResponse create(ProjectIdentifier pid, CreateTagCommand cmd, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        tagValidator.ensureUniqueName(project, cmd.name());

        Tag tag = Tag.create(project, cmd.name(), cmd.description(), cmd.color());
        tagRepository.save(tag);

        return TagResponse.from(tag);
    }

    @Override
    public void rename(ProjectIdentifier pid, Long tagId, String newName, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Tag tag = tagFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), tagId);

        var name = Name.of(newName);
        if (Objects.equals(tag.getName().getDisplay(), name.getDisplay())) {
            return;
        }

        tagValidator.ensureUniqueName(tag.getProject(), name);
        tag.rename(name);
    }

    @Override
    public void update(ProjectIdentifier pid, Long tagId, UpdateTagCommand cmd, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Tag tag = tagFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), tagId);

        Patchers.apply(cmd.description(), tag::updateDescription);
        Patchers.apply(cmd.color(), tag::updateColor);
    }

    @Override
    public void delete(ProjectIdentifier pid, Long tagId, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Tag tag = tagFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), tagId);
        issueTagRepository.deleteAllByTag(tag);

        tagRepository.delete(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDetail> getTagsByProject(ProjectIdentifier pid, Long actorMemberId) {
        projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        List<Tag> tags = tagFinder.getAllBy(pid.workspaceKey(), pid.projectKey());
        return tags.stream().map(TagDetail::from).toList();
    }
}
