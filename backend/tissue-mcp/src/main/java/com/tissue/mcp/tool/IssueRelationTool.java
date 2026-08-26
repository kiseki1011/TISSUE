package com.tissue.mcp.tool;

import com.tissue.feature.issue.application.port.usecase.IssueRelationUseCase;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.dto.IssueIdentifier;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueRelationTool {

    private final IssueRelationUseCase issueRelationUseCase;

    @McpTool(name = "add_relation", description = """
            Link two issues, to record that one blocks, causes or duplicates another, or is simply \
            related to it. Direction runs from the issue you name first to the target: "PROJ-1 BLOCKS \
            PROJ-2" means PROJ-1 has to be finished before PROJ-2 can be. Read both issues first so the \
            direction is the one you mean - it is what a blocking guard will act on.

            Rejected when the pair is already linked, when the two keys are the same, or when a \
            directional link would close a cycle (PROJ-1 blocks PROJ-2 blocks PROJ-1).""")
    public void addRelation(
            @McpToolParam(required = true, description = "The issue the relation starts from, ex: \"PROJ-1\".")
                    String issueKey,
            @McpToolParam(required = true, description = "The issue at the other end, ex: \"PROJ-2\".")
                    String targetIssueKey,
            @McpToolParam(required = true, description = """
                            One of:
                            - "BLOCKS": this issue must be done before the target can be
                            - "CAUSES": this issue is why the target exists
                            - "DUPLICATES": this issue is a duplicate of the target
                            - "RELEVANT": the two are related, with no direction implied""") IssueRelationType relationType) {
        McpActor.requireWriteScope();

        issueRelationUseCase.add(
                IssueIdentifier.ofIssueKey(issueKey), targetIssueKey, relationType, McpActor.currentMemberId());
    }

    @McpTool(name = "remove_relation", description = """
            Unlink two issues. One relation exists per pair, so the target key alone identifies it and \
            no relation type is needed.

            Remove it from the issue the link starts at. A directional relation belongs to its source: \
            if PROJ-1 blocks PROJ-2, only PROJ-1 can drop it, and asking from PROJ-2 reports that no such \
            relation exists even though PROJ-2 plainly shows it as "blocked by". RELEVANT is the \
            exception - being undirected, either side can remove it. get_issue with the relations \
            section tells you which side you are on: the plain groups (blocks, causes, duplicates) are \
            yours to remove, the "...by" groups belong to the other issue.""")
    public void removeRelation(
            @McpToolParam(required = true, description = "The issue the relation starts from, ex: \"PROJ-1\".")
                    String issueKey,
            @McpToolParam(required = true, description = "The issue at the other end, ex: \"PROJ-2\".")
                    String targetIssueKey) {
        McpActor.requireWriteScope();

        issueRelationUseCase.remove(IssueIdentifier.ofIssueKey(issueKey), targetIssueKey, McpActor.currentMemberId());
    }
}
