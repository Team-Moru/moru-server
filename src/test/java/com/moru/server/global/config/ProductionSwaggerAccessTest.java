package com.moru.server.global.config;

import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"prod", "test"})
@SpringBootTest
@AutoConfigureMockMvc
class ProductionSwaggerAccessTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwt;

    @ParameterizedTest
    @ValueSource(strings = {"/swagger-ui.html", "/swagger-ui/index.html",
            "/v3/api-docs", "/v3/api-docs/swagger-config"})
    void deniesSwaggerInProduction(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + jwt.createAccessToken(1L, Role.ADMIN)))
                .andExpect(status().isForbidden());
    }
}
