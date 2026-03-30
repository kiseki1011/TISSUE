package com.tissue.feature.tag.web;

import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.feature.tag.application.service.TagService;
import com.tissue.feature.tag.web.request.CreateTagRequest;
import com.tissue.feature.tag.web.request.RenameTagRequest;
import com.tissue.feature.tag.web.request.UpdateTagRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<TagResponse> create(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateTagRequest req,
            @CurrentMember MemberDetails memberDetails) {
        var command = req.toCommand();
        TagResponse response =
                tagService.create(ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.tagId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{tagId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long tagId,
            @RequestBody @Valid RenameTagRequest request,
            @CurrentMember MemberDetails memberDetails) {
        tagService.rename(
                ProjectIdentifier.of(workspaceKey, projectKey), tagId, request.name(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{tagId}")
    public ResponseEntity<Void> update(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long tagId,
            @RequestBody @Valid UpdateTagRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        tagService.update(ProjectIdentifier.of(workspaceKey, projectKey), tagId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> delete(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long tagId,
            @CurrentMember MemberDetails memberDetails) {
        tagService.delete(ProjectIdentifier.of(workspaceKey, projectKey), tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TagDetail>> getTagsByProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        List<TagDetail> tags = tagService.getTagsByProject(
                ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.ok(tags);
    }
}
