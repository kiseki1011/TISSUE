package deprecated.com.tissue.unit.controller;

import deprecated.com.tissue.support.helper.ControllerTestHelper;

class WorkspaceControllerTest extends ControllerTestHelper {

	// static Stream<Arguments> provideInvalidInputs() {
	// 	return Stream.of(
	// 		arguments(null, null), // null
	// 		arguments("", ""),   // 빈 문자열
	// 		arguments(" ", " ")  // 공백
	// 	);
	// }
	//
	// @Test
	// @DisplayName("POST /workspaces - 워크스페이스 생성을 성공하면 CREATED")
	// void createWorkspace_success_CREATED() throws Exception {
	// 	// given
	// 	CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
	// 		.name("Test Workspace")
	// 		.description("Test Description")
	// 		.build();
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isCreated())
	// 		.andDo(print());
	// }
	//
	// @ParameterizedTest
	// @MethodSource("provideInvalidInputs")
	// @DisplayName("POST /workspaces - 워크스페이스 생성 요청에서 이름 또는 설명이 널(null), 빈 문자열(''), 공백(' ')이면 검증을 실패한다")
	// void createWorkspace_fail_nameAndDescription_notBlankValidation(String name, String description) throws Exception {
	// 	// given
	// 	CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
	// 		.name(name)
	// 		.description(description)
	// 		.build();
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isBadRequest())
	// 		.andExpect(jsonPath("$.message").value("One or more fields have failed validation."))
	// 		.andDo(print());
	// }
	//
	// private String createLongString(int length) {
	// 	return "a".repeat(Math.max(0, length));
	// }
	//
	// @Test
	// @DisplayName("POST /workspaces - 워크스페이스 생성 요청에서 이름의 범위는 2~50자, 설명은 1~255자를 지켜야 한다")
	// void createWorkspace_fail_nameAndDescription_sizeValidation() throws Exception {
	// 	// given
	// 	String longName = createLongString(51);
	// 	String longDescription = createLongString(256);
	//
	// 	String nameValidMsg = messageSource.getMessage("valid.size.name", null, Locale.ENGLISH);
	// 	String descriptionValidMsg = messageSource.getMessage("valid.size.standard", null, Locale.ENGLISH);
	//
	// 	CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
	// 		.name(longName)
	// 		.description(longDescription)
	// 		.build();
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/workspaces")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.header("Accept-Language", "en")
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isBadRequest())
	// 		.andExpect(jsonPath("$.data[*].message").value(hasItem(nameValidMsg)))
	// 		.andExpect(jsonPath("$.data[*].message").value(hasItem(descriptionValidMsg)))
	// 		.andDo(print());
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{workspaceKey}/info - 워크스페이스 정보 수정 요청에 성공하면 OK를 응답한다")
	// void updateWorkspaceContent_shouldReturnUpdatedContent() throws Exception {
	// 	// given
	// 	UpdateWorkspaceInfoRequest request = new UpdateWorkspaceInfoRequest("New Title", "New Description");
	//
	// 	String workspaceCode = "TESTCODE";
	//
	// 	Workspace workspace = Workspace.builder()
	// 		.code(workspaceCode)
	// 		.name("New Title")
	// 		.description("New Description")
	// 		.build();
	//
	// 	WorkspaceResponse response = WorkspaceResponse.from(workspace);
	//
	// 	when(workspaceCommandService.updateWorkspaceInfo(
	// 		ArgumentMatchers.any(UpdateWorkspaceInfoRequest.class),
	// 		eq(workspaceCode)))
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{workspaceKey}/info", workspaceCode)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Workspace info updated."))
	// 		.andExpect(jsonPath("$.data.workspaceKey").value(workspaceCode))
	// 		.andDo(print());
	//
	// 	verify(workspaceCommandService, times(1))
	// 		.updateWorkspaceInfo(ArgumentMatchers.any(UpdateWorkspaceInfoRequest.class), eq(workspaceCode));
	// }
	//
	// @Test
	// @DisplayName("DELETE /workspaces/{workspaceKey} - 워크스페이스 삭제 요청에 성공하면 OK")
	// void deleteWorkspace_shouldReturnSuccess() throws Exception {
	// 	// given
	// 	DeleteWorkspaceRequest request = new DeleteWorkspaceRequest("password1234!");
	//
	// 	Workspace workspace = Workspace.builder()
	// 		.code("TESTCODE")
	// 		.build();
	//
	// 	// when(workspaceCommandService.deleteWorkspace(eq("TESTCODE"), anyLong()))
	// 	// 	.thenReturn();
	//
	// 	// when & then
	// 	mockMvc.perform(delete("/api/v1/workspaces/{workspaceKey}", "TESTCODE")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isNoContent())
	// 		.andExpect(jsonPath("$.message").value("Workspace deleted."));
	//
	// 	verify(workspaceCommandService, times(1))
	// 		.deleteWorkspace(eq("TESTCODE"), anyLong());
	// }
	//
	// @Test
	// @DisplayName("GET /workspaces/{code} - 워크스페이스 상세 정보 조회를 성공하면 OK")
	// void test5() throws Exception {
	// 	// given
	// 	String code = "ABCD1234";
	//
	// 	WorkspaceDetail workspaceDetail = WorkspaceDetail.builder()
	// 		.name("Test Workspace")
	// 		.description("Test Description")
	// 		.code(code)
	// 		.build();
	//
	// 	// MockHttpSession session = new MockHttpSession();
	// 	// session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);
	//
	// 	when(workspaceQueryService.getWorkspaceDetail(code))
	// 		.thenReturn(workspaceDetail);
	//
	// 	// when & then
	// 	mockMvc.perform(get("/api/v1/workspaces/{code}", code))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.data.code").value(code))
	// 		.andExpect(jsonPath("$.data.name").value("Test Workspace"))
	// 		.andExpect(jsonPath("$.data.description").value("Test Description"))
	// 		.andDo(print());
	//
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{workspaceKey}/key - 이슈 키 접두사(issue key prefix) 수정에 성공하면 OK")
	// void updateWorkspaceIssueKeyPrefix_shouldReturnOK() throws Exception {
	// 	// given
	// 	UpdateIssueKeyRequest request = new UpdateIssueKeyRequest("TESTPREFIX");
	//
	// 	String workspaceCode = "TESTCODE";
	//
	// 	WorkspaceResponse response = new WorkspaceResponse(workspaceCode);
	//
	// 	when(workspaceCommandService.updateIssueKeyPrefix(workspaceCode, request))
	// 		.thenReturn(response);
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{workspaceKey}/key", workspaceCode)
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.message").value("Issue key prefix updated."))
	// 		.andExpect(jsonPath("$.data.workspaceKey").value(workspaceCode))
	// 		.andDo(print());
	//
	// 	verify(workspaceCommandService, times(1))
	// 		.updateIssueKeyPrefix("TESTCODE", request);
	// }
	//
	// @Test
	// @DisplayName("PATCH /workspaces/{workspaceKey}/key - 이슈 키 접두사(issue key prefix) 수정 요청에 영문자를 사용하지 않으면 검증을 실패한다")
	// void updateWorkspaceIssueKeyPrefix_failsRequestValidation() throws Exception {
	// 	// given
	// 	UpdateIssueKeyRequest request = new UpdateIssueKeyRequest("잘못된접두사");
	//
	// 	// when & then
	// 	mockMvc.perform(patch("/api/v1/workspaces/{workspaceKey}/key", "TESTCODE")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isBadRequest())
	// 		.andDo(print());
	// }
}
