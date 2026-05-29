package com.tissue.feature.tag.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.feature.tag.application.port.usecase.TagCommandUseCase;
import com.tissue.feature.tag.domain.exception.TagErrorCode;
import com.tissue.feature.tag.web.request.CreateTagRequest;
import com.tissue.feature.tag.web.request.UpdateTagRequest;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.TagErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tag")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TagCommandController {

    private final TagCommandUseCase tagCommandUseCase;

    @Operation(operationId = "createTag", summary = "Create tag", description = """
                Create a new tag within a project.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tag created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @TagErrors({TagErrorCode.DUPLICATE_TAG_NAME})
    @PostMapping("projects/{projectKey}/tags")
    public ResponseEntity<TagResponse> createTag(
            @PathVariable String projectKey,
            @RequestBody @Valid CreateTagRequest req,
            @CurrentMember MemberDetails memberDetails) {
        var command = req.toCommand();
        TagResponse response = tagCommandUseCase.create(
                ProjectIdentifier.ofProjectKey(projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateTag", summary = "Update tag", description = """
                Update a tag's name, description, or color. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @TagErrors({
        TagErrorCode.TAG_NOT_FOUND,
        TagErrorCode.DUPLICATE_TAG_NAME,
    })
    @PatchMapping("tags/{tagId}")
    public ResponseEntity<Void> updateTag(
            @PathVariable Long tagId,
            @RequestBody @Valid UpdateTagRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        tagCommandUseCase.update(tagId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteTag", summary = "Delete tag", description = """
                Permanently delete a tag from the project.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @TagErrors({TagErrorCode.TAG_NOT_FOUND})
    @DeleteMapping("tags/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId, @CurrentMember MemberDetails memberDetails) {
        tagCommandUseCase.delete(tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
