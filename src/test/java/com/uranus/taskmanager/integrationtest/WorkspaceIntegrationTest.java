package com.uranus.taskmanager.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    @BeforeEach
    void setup() {
        workspaceRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /workspaces: 응답의 필드가 제공한 값과 일치하고, workspaceCode는 존재해야한다.")
    public void 테스트1() throws Exception {

        WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
                .name("Test Workspace")
                .description("Test Description")
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Workspace"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.workspaceCode").exists())
                .andDo(print());
    }

    @Test
    @DisplayName("POST /workspaces: DB에 하나의 값만 저장된다.")
    public void 테스트2() throws Exception {

        WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
                .name("Test Workspace")
                .description("Test Description")
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andDo(print());

        assertThat(workspaceRepository.count()).isEqualTo(1L);
    }

    @Test
    public void 테스트3() {

    }

    @Test
    public void 테스트4() {

    }

    @Test
    public void 테스트5() {

    }

}