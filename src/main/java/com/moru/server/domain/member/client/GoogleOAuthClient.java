package com.moru.server.domain.member.client;

import java.security.Key;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import org.springframework.stereotype.Component;

import com.moru.server.global.config.OAuthProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Component
public class GoogleOAuthClient {

    private static final String RSA_SHA_256_ALGORITHM = "RS256";

    private final OAuthJwksClient oauthJwksClient;
    private final OAuthProperties oauthProperties;

    public GoogleOAuthClient(
            OAuthJwksClient oauthJwksClient,
            OAuthProperties oauthProperties
    ) {
        this.oauthJwksClient = oauthJwksClient;
        this.oauthProperties = oauthProperties;
    }

    public GoogleMemberInfo getMemberInfo(String idToken) {
        validateOAuthConfig();

        Claims claims = parseClaims(idToken);

        return new GoogleMemberInfo(
                getRequiredSubject(claims),
                extractNickname(claims)
        );
    }

    private void validateOAuthConfig() {
        if (!oauthProperties.getGoogle().hasClientId()) {
            throw new BusinessException(ErrorStatus.OAUTH_CONFIG_MISSING);
        }
    }

    private Claims parseClaims(String idToken) {
        try {
            return Jwts.parser()
                    .keyLocator(new LocatorAdapter<>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            validateAlgorithm(header);
                            return oauthJwksClient.getPublicKey(
                                    oauthProperties.getGoogle().getJwksUri(),
                                    header.getKeyId()
                            );
                        }
                    })
                    .requireIssuer(oauthProperties.getGoogle().getIssuer())
                    .requireAudience(oauthProperties.getGoogle().getClientId())
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private void validateAlgorithm(JwsHeader header) {
        if (!RSA_SHA_256_ALGORITHM.equals(header.getAlgorithm())) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private String getRequiredSubject(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new BusinessException(ErrorStatus.INVALID_TOKEN);
        }

        return subject;
    }

    private String extractNickname(Claims claims) {
        String name = claims.get("name", String.class);

        if (name != null && !name.isBlank()) {
            return name;
        }

        return claims.get("email", String.class);
    }

    public record GoogleMemberInfo(
            String oauthId,
            String nickname
    ) {
    }
}
