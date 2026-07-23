package com.moru.server.domain.member.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.service.command.auth.AuthCommandService;
import com.moru.server.global.security.auth.DevAuthSecretFilter;

@ActiveProfiles({"prod", "test"})
@SpringBootTest(properties = "dev-auth.secret=prod-dev-auth-secret")
@AutoConfigureMockMvc
class DevAuthControllerTest {

    private static final String DEV_AUTH_SECRET = "prod-dev-auth-secret";
    private static final String REQUEST_BODY = """
            {
              "oauthId": "test-user-1",
              "nickname": "테스트회원"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthCommandService authCommandService;

    @Test
    void rejectsDevTokenRequestWithoutSecretInProdProfile() throws Exception {
        mockMvc.perform(createDevTokenRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON403"));

        verifyNoInteractions(authCommandService);
    }

    @Test
    void rejectsDevTokenRequestWithInvalidSecretInProdProfile() throws Exception {
        mockMvc.perform(createDevTokenRequest()
                        .header(DevAuthSecretFilter.SECRET_HEADER, "invalid-secret"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON403"));

        verifyNoInteractions(authCommandService);
    }

    @Test
    void issuesDevTokenWithValidSecretThroughSecurityFilterChain() throws Exception {
        AuthRequestDTO.DevTokenRequest request =
                new AuthRequestDTO.DevTokenRequest("test-user-1", "테스트회원");
        AuthResponseDTO.TokenResponse response = AuthResponseDTO.TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .memberId(1L)
                .onboardingCompleted(false)
                .build();

        when(authCommandService.issueDevToken(request)).thenReturn(response);

        mockMvc.perform(createDevTokenRequest()
                        .header(DevAuthSecretFilter.SECRET_HEADER, DEV_AUTH_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.result.memberId").value(1));

        verify(authCommandService).issueDevToken(request);
    }

    private MockHttpServletRequestBuilder createDevTokenRequest() {
        return post("/dev/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY);
    }
}
