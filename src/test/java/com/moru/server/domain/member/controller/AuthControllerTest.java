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

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.enums.OAuthProvider;
import com.moru.server.domain.member.service.command.auth.AuthCommandService;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthCommandService authCommandService;

    @Test
    void acceptsIdentityTokenForAppleLogin() throws Exception {
        AuthRequestDTO.SocialLoginRequest request =
                new AuthRequestDTO.SocialLoginRequest("apple-identity-token", "apple-authorization-code");
        when(authCommandService.loginWithSocial(OAuthProvider.APPLE, request))
                .thenReturn(socialLoginResponse());

        mockMvc.perform(post("/auth/login/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "apple-identity-token",
                                  "authorizationCode": "apple-authorization-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.memberId").value(1));

        verify(authCommandService).loginWithSocial(OAuthProvider.APPLE, request);
    }

    @Test
    void keepsSupportingTokenForAppleLogin() throws Exception {
        AuthRequestDTO.SocialLoginRequest request =
                new AuthRequestDTO.SocialLoginRequest("apple-identity-token", "apple-authorization-code");
        when(authCommandService.loginWithSocial(OAuthProvider.APPLE, request))
                .thenReturn(socialLoginResponse());

        mockMvc.perform(post("/auth/login/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "apple-identity-token",
                                  "authorizationCode": "apple-authorization-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(authCommandService).loginWithSocial(OAuthProvider.APPLE, request);
    }

    @Test
    void rejectsAppleLoginWithoutSocialToken() throws Exception {
        mockMvc.perform(post("/auth/login/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorizationCode": "apple-authorization-code"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400"));

        verifyNoInteractions(authCommandService);
    }

    private AuthResponseDTO.SocialLoginResponse socialLoginResponse() {
        return AuthResponseDTO.SocialLoginResponse.builder()
                .memberId(1L)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .isNewMember(true)
                .onboardingCompleted(false)
                .build();
    }
}
