package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.port.usecase.IssueTypeUseCase;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.SystemRoleAuthorizationService;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.Workflow;
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
public class IssueTypeService implements IssueTypeUseCase {

    private final WorkflowFinder workflowFinder;
    private final IssueTypeFinder issueTypeFinder;
    private final MemberFinder memberFinder;
    private final IssueTypeRepository issueTypeRepository;
    private final IssueTypeValidator issueTypeValidator;
    private final SystemRoleAuthorizationService systemRoleAuthorizationService;

    @Override
    public IssueTypeResponse create(CreateIssueTypeCommand cmd, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        Workflow workflow = workflowFinder.getById(cmd.workflowId());

        issueTypeValidator.ensureUniqueLabel(cmd.name());

        IssueType issueType = IssueType.create(
                cmd.name(), cmd.description(), cmd.color(), cmd.icon(), cmd.issueHierarchy(), workflow);

        IssueType savedType = issueTypeRepository.save(issueType);

        return IssueTypeResponse.from(savedType);
    }

    @Override
    public void update(Long issueTypeId, PatchIssueTypeCommand cmd, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getById(issueTypeId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        Patchers.apply(cmd.name(), newName -> {
            Name name = Name.of(newName);
            if (!isNameUnchanged(issueType, name)) {
                issueTypeValidator.ensureUniqueLabel(name);
                issueType.rename(name);
            }
        });
        Patchers.apply(cmd.description(), issueType::updateDescription);
        Patchers.apply(cmd.color(), issueType::updateColor);
        Patchers.apply(cmd.icon(), issueType::updateIcon);
    }

    @Override
    public void delete(Long issueTypeId, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getById(issueTypeId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        issueTypeValidator.ensureDeletable(issueType);

        issueTypeRepository.delete(issueType);
    }

    @Override
    public void reorderFields(Long issueTypeId, List<Long> orderedIds, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getById(issueTypeId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        issueType.reorderFields(orderedIds);
    }

    private boolean isNameUnchanged(IssueType it, Name newName) {
        return Objects.equals(it.getName(), newName.toString());
    }
}
