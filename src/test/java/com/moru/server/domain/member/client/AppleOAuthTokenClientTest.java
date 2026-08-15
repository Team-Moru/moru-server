package com.moru.server.domain.member.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.moru.server.global.config.OAuthProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class AppleOAuthTokenClientTest {

    private static final String CLIENT_ID = "com.teammoru.Moru";
    private static final String TEAM_ID = "TEAM123456";
    private static final String KEY_ID = "KEY123456";

    private MockRestServiceServer mockServer;
    private AppleOAuthTokenClient tokenClient;
    private AppleClientSecretGenerator clientSecretGenerator;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();

        OAuthProperties oauthProperties = new OAuthProperties();
        oauthProperties.getApple().setClientId(CLIENT_ID);
        oauthProperties.getApple().setTeamId(TEAM_ID);
        oauthProperties.getApple().setKeyId(KEY_ID);
        oauthProperties.getApple().setPrivateKey(toPem(keyPair));

        clientSecretGenerator = new AppleClientSecretGenerator(oauthProperties);
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        tokenClient = new AppleOAuthTokenClient(restClientBuilder, oauthProperties, clientSecretGenerator);
    }

    @Test
    void generatesSignedAppleClientSecret() {
        String clientSecret = clientSecretGenerator.generate();

        Claims claims = Jwts.parser()
                .verifyWith((ECPublicKey) keyPair.getPublic())
                .requireIssuer(TEAM_ID)
                .requireSubject(CLIENT_ID)
                .requireAudience("https://appleid.apple.com")
                .build()
                .parseSignedClaims(clientSecret)
                .getPayload();

        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
    }

    @Test
    void exchangesAuthorizationCodeForRefreshToken() {
        mockServer.expect(once(), requestTo("https://appleid.apple.com/auth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(allOf(
                        containsString("client_id=" + CLIENT_ID),
                        containsString("code=apple-authorization-code"),
                        containsString("grant_type=authorization_code")
                )))
                .andRespond(withSuccess(
                        "{\"refresh_token\":\"apple-refresh-token\",\"id_token\":\"apple-id-token\"}",
                        MediaType.APPLICATION_JSON
                ));

        AppleOAuthTokenClient.AppleTokens tokens =
                tokenClient.exchangeAuthorizationCode("apple-authorization-code");

        assertThat(tokens.refreshToken()).isEqualTo("apple-refresh-token");
        assertThat(tokens.idToken()).isEqualTo("apple-id-token");
        mockServer.verify();
    }

    @Test
    void revokesAppleRefreshToken() {
        mockServer.expect(once(), requestTo("https://appleid.apple.com/auth/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("token=apple-refresh-token"),
                        containsString("token_type_hint=refresh_token")
                )))
                .andRespond(withSuccess());

        tokenClient.revokeRefreshToken("apple-refresh-token");

        mockServer.verify();
    }

    @Test
    void treatsAlreadyRevokedRefreshTokenAsSuccess() {
        mockServer.expect(once(), requestTo("https://appleid.apple.com/auth/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_token\",\"error_description\":\"Token is inactive\"}"));

        tokenClient.revokeRefreshToken("already-revoked-refresh-token");

        mockServer.verify();
    }

    @Test
    void doesNotIgnoreOtherAppleRevokeErrors() {
        mockServer.expect(once(), requestTo("https://appleid.apple.com/auth/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));

        assertThatThrownBy(() -> tokenClient.revokeRefreshToken("apple-refresh-token"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.APPLE_REVOKE_FAILED));

        mockServer.verify();
    }

    @Test
    void rejectsBlankAuthorizationCodeWithoutCallingApple() {
        assertThatThrownBy(() -> tokenClient.exchangeAuthorizationCode(" "))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode())
                                .isEqualTo(ErrorStatus.APPLE_AUTHORIZATION_CODE_INVALID));
    }

    private String toPem(KeyPair pair) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(pair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
    }
}
