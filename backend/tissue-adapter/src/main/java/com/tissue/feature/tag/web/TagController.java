package com.tissue.feature.tag.web;

import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.feature.tag.application.service.TagService;
import com.tissue.feature.tag.web.request.CreateTagRequest;
import com.tissue.feature.tag.web.request.UpdateTagRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Tag")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "Create tag", description = """
                Create a new tag within a project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tag created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Tag name already exists", content = @Content)
    })
    @PostMapping("projects/{projectKey}/tags")
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

    @Operation(summary = "Update tag", description = """
                Update a tag's name, description, or color. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Tag name already exists", content = @Content)
    })
    @PatchMapping("tags/{tagId}")
    public ResponseEntity<Void> update(
            @PathVariable String workspaceKey,
            @PathVariable Long tagId,
            @RequestBody @Valid UpdateTagRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        tagService.update(workspaceKey, tagId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete tag", description = """
                Delete a tag from the project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
    })
    @DeleteMapping("tags/{tagId}")
    public ResponseEntity<Void> delete(
            @PathVariable String workspaceKey, @PathVariable Long tagId, @CurrentMember MemberDetails memberDetails) {
        tagService.delete(workspaceKey, tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List tags", description = "Retrieve all tags in the project.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tags retrieved"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @GetMapping("projects/{projectKey}/tags")
    public ResponseEntity<List<TagDetail>> getTagsByProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        List<TagDetail> tags = tagService.getTagsByProject(
                ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.ok(tags);
    }
}
