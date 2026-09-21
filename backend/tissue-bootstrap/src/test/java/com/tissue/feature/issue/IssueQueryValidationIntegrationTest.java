package com.tissue.feature.issue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.service.AuthenticationService;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Bad query-parameter input on the issue query endpoints must answer 400, not 500.
 */
@AutoConfigureMockMvc
class IssueQueryValidationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private AuthenticationIdentityRepository identityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationService authenticationService;

    private String bearer;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.create("v@tissue.com", "vuser", "Validator"));
        identityRepository.save(AuthenticationIdentity.createEmailIdentity(
                member, "v@tissue.com", passwordEncoder.encode("password1234")));
        bearer = "Bearer "
                + authenticationService
                        .login("v@tissue.com", "password1234", "127.0.0.1")
                        .accessToken();
    }

    @Test
    @DisplayName("detail view: a non-positive commentSize is 400, not 500")
    void detailRejectsNonPositiveCommentSize() throws Exception {
        mockMvc.perform(get("/api/v1/issues/PROJ-1/detail")
                        .param("commentSize", "0")
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("detail view: an over-limit commentSize is 400, not an unbounded fetch")
    void detailRejectsOverLimitCommentSize() throws Exception {
        mockMvc.perform(get("/api/v1/issues/PROJ-1/detail")
                        .param("commentSize", "9999")
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search: a member-id filter that is neither 'me' nor numeric is 400, not 500")
    void searchRejectsNonNumericMemberIdFilter() throws Exception {
        mockMvc.perform(get("/api/v1/issues:search")
                        .param("assigneeMemberIds", "abc")
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest());
    }
}
