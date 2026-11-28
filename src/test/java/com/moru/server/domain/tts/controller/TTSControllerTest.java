package com.moru.server.domain.tts.controller;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.repository.TTSRepository;
import com.moru.server.global.security.jwt.JwtProperties;
import com.moru.server.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TTSControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwt;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private TTSRepository voices;

    @BeforeEach
    void setUp() {
        when(voices.findAllByOrderByIdAsc()).thenReturn(List.of(
                TTS.builder().id(1L).name("Leda").label("산뜻").build(),
                TTS.builder().id(2L).name("Despina").label("Despina").v2Enabled(false).build()));
    }

    @Test
    void publicCatalogIsAvailableWithoutAValidMemberToken() throws Exception {
        var expiredProperties = new JwtProperties();
        expiredProperties.setSecret(jwtProperties.getSecret());
        expiredProperties.setAccessTokenExpiration(Duration.ofMinutes(-1));
        String expired = new JwtTokenProvider(expiredProperties).createInstallationAccessToken(UUID.randomUUID());
        String installationToken = jwt.createInstallationAccessToken(UUID.randomUUID());

        mockMvc.perform(get("/tts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voices.length()").value(1))
                .andExpect(jsonPath("$.result.voices[0].ttsId").value(1));
        mockMvc.perform(get("/tts").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voices.length()").value(1))
                .andExpect(jsonPath("$.result.voices[0].ttsId").value(1));
        mockMvc.perform(get("/tts").header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voices.length()").value(1))
                .andExpect(jsonPath("$.result.voices[0].ttsId").value(1));
        mockMvc.perform(get("/members/me/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH4013"));
    }

    @Test
    void authenticatedMemberCanStillResolvePreviouslySelectedVoices() throws Exception {
        var token = jwt.createAccessToken(1L, Role.MEMBER);

        mockMvc.perform(get("/tts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voices.length()").value(2))
                .andExpect(jsonPath("$.result.voices[0].ttsId").value(1))
                .andExpect(jsonPath("$.result.voices[1].ttsId").value(2));
    }
}
