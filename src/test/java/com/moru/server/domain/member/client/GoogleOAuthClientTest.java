package com.moru.server.domain.member.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.moru.server.global.config.OAuthProperties;

class GoogleOAuthClientTest {

    private static final String CLIENT_ID = "google-client-id";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String KEY_ID = "google-key-id";

    private MockRestServiceServer mockServer;
    private GoogleOAuthClient googleOAuthClient;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        OAuthProperties oauthProperties = new OAuthProperties();
        oauthProperties.getGoogle().setClientId(CLIENT_ID);
        oauthProperties.getGoogle().setIssuer(ISSUER);
        oauthProperties.getGoogle().setJwksUri(JWKS_URI);

        OAuthJwksClient oauthJwksClient = new OAuthJwksClient(restClientBuilder);
        googleOAuthClient = new GoogleOAuthClient(oauthJwksClient, oauthProperties);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
    }

    @Test
    void verifiesGoogleIdTokenLocallyAndCachesJwks() {
        mockServer.expect(once(), requestTo(JWKS_URI))
                .andRespond(withSuccess(createJwksResponse(), MediaType.APPLICATION_JSON));

        String idToken = createIdToken();

        GoogleOAuthClient.GoogleMemberInfo firstResult = googleOAuthClient.getMemberInfo(idToken);
        GoogleOAuthClient.GoogleMemberInfo secondResult = googleOAuthClient.getMemberInfo(idToken);

        assertThat(firstResult.oauthId()).isEqualTo("google-member-id");
        assertThat(firstResult.nickname()).isEqualTo("모루");
        assertThat(secondResult).isEqualTo(firstResult);
        mockServer.verify();
    }

    private String createIdToken() {
        Instant now = Instant.now();

        return Jwts.builder()
                .header()
                .keyId(KEY_ID)
                .and()
                .issuer(ISSUER)
                .audience()
                .add(CLIENT_ID)
                .and()
                .subject("google-member-id")
                .claim("name", "모루")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private String createJwksResponse() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        return """
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "kid": "%s",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """.formatted(
                KEY_ID,
                encodeBase64Url(publicKey.getModulus()),
                encodeBase64Url(publicKey.getPublicExponent())
        );
    }

    private String encodeBase64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] unsignedBytes = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, unsignedBytes, 0, unsignedBytes.length);
            bytes = unsignedBytes;
        }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
