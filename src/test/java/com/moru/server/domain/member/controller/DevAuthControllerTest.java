package com.moru.server.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.service.command.auth.AuthCommandService;

class DevAuthControllerTest {

    @Test
    void issuesDevTokenWhenProdProfileIsActive() throws Exception {
        AuthCommandService authCommandService = mock(AuthCommandService.class);
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

        try (AnnotationConfigApplicationContext context = createProdContext(authCommandService)) {
            assertThat(context.getBeansOfType(DevAuthController.class)).hasSize(1);

            MockMvc mockMvc = MockMvcBuilders
                    .standaloneSetup(context.getBean(DevAuthController.class))
                    .build();

            mockMvc.perform(post("/dev/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "oauthId": "test-user-1",
                                      "nickname": "테스트회원"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.result.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.result.memberId").value(1));
        }

        verify(authCommandService).issueDevToken(request);
    }

    private AnnotationConfigApplicationContext createProdContext(AuthCommandService authCommandService) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("prod");
        context.registerBean(AuthCommandService.class, () -> authCommandService);
        context.register(DevAuthController.class);
        context.refresh();
        return context;
    }
}
