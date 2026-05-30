package com.tissue.feature.tag.application.service;

import com.tissue.feature.issue.application.port.repository.IssueTagRepository;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.feature.tag.application.dto.request.UpdateTagCommand;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.application.port.usecase.TagCommandUseCase;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TagCommandService implements TagCommandUseCase {

    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectFinder projectFinder;
    private final TagFinder tagFinder;
    private final TagRepository tagRepository;
    private final TagValidator tagValidator;
    private final IssueTagRepository issueTagRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public TagResponse create(ProjectIdentifier pid, CreateTagCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(pid.projectKey(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getByProjectKey(pid.projectKey());
        tagValidator.ensureUniqueName(project, cmd.name());

        Tag tag = Tag.create(project, cmd.name(), cmd.description(), cmd.color());
        tagRepository.save(tag);

        return TagResponse.from(tag);
    }

    @Override
    public void update(Long tagId, UpdateTagCommand cmd, Long actorMemberId) {
        Tag tag = tagFinder.getWithProject(tagId);
        String projectKey = tag.getProject().getKey();

        ProjectMember actor = projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.name(), newName -> {
            var name = Name.of(newName);
            if (!Objects.equals(tag.getName(), name)) {
                tagValidator.ensureUniqueName(tag.getProject(), name);
                tag.rename(name);
            }
        });
        Patchers.apply(cmd.description(), tag::updateDescription);
        Patchers.apply(cmd.color(), tag::updateColor);
    }

    @Override
    public void delete(Long tagId, Long actorMemberId) {
        Tag tag = tagFinder.getWithProject(tagId);
        String projectKey = tag.getProject().getKey();

        ProjectMember actor = projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        issueTagRepository.deleteAllByTag(tag);

        tagRepository.delete(tag);
    }
}
