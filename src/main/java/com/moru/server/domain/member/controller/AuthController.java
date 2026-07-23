package com.moru.server.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.enums.OAuthProvider;
import com.moru.server.domain.member.service.command.auth.AuthCommandService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.CurrentMemberProvider;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String X_REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private static final String SOCIAL_LOGIN_DESCRIPTION = """
            소셜 플랫폼에서 발급받은 토큰을 검증한 뒤 회원가입 또는 로그인을 처리합니다.

            - Authorization 헤더 없이 호출합니다.
            - kakao: token에 카카오 accessToken을 전달합니다.
            - google: token에 Google idToken을 전달합니다.
            - apple: token에 Apple identityToken을 전달합니다.
            """;
    private static final String KAKAO_REQUEST_EXAMPLE = """
            {
              "token": "kakao_access_token",
              "authorizationCode": null
            }
            """;
    private static final String GOOGLE_REQUEST_EXAMPLE = """
            {
              "token": "google_id_token",
              "authorizationCode": null
            }
            """;
    private static final String APPLE_REQUEST_EXAMPLE = """
            {
              "token": "apple_identity_token",
              "authorizationCode": "apple_authorization_code"
            }
            """;
    private static final String SOCIAL_LOGIN_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "code": "COMMON200",
              "message": "성공입니다.",
              "result": {
                "memberId": 1,
                "accessToken": "jwt_access_token",
                "refreshToken": "jwt_refresh_token",
                "isNewMember": true,
                "onboardingCompleted": false
              }
            }
            """;
    private static final String TOKEN_REISSUE_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "code": "COMMON200",
              "message": "성공입니다.",
              "result": {
                "accessToken": "new_jwt_access_token",
                "refreshToken": "new_jwt_refresh_token",
                "tokenType": "Bearer",
                "memberId": 1,
                "onboardingCompleted": false
              }
            }
            """;
    private static final String LOGOUT_REQUEST_EXAMPLE = """
            {
              "refreshToken": "jwt_refresh_token"
            }
            """;
    private static final String LOGOUT_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "code": "COMMON200",
              "message": "성공입니다."
            }
            """;
    private static final String INVALID_REFRESH_TOKEN_EXAMPLE = """
            {
              "isSuccess": false,
              "code": "AUTH4002",
              "message": "로그인 정보가 만료되었습니다."
            }
            """;
    private static final String INVALID_PROVIDER_EXAMPLE = """
            {
              "isSuccess": false,
              "code": "AUTH4014",
              "message": "지원하지 않는 소셜 로그인 제공자입니다."
            }
            """;
    private static final String INVALID_SOCIAL_TOKEN_EXAMPLE = """
            {
              "isSuccess": false,
              "code": "AUTH4004",
              "message": "유효하지 않은 토큰입니다."
            }
            """;
    private static final String OAUTH_CONFIG_MISSING_EXAMPLE = """
            {
              "isSuccess": false,
              "code": "AUTH5001",
              "message": "OAuth 설정값이 누락되었습니다."
            }
            """;

    private final AuthCommandService authCommandService;
    private final CurrentMemberProvider currentMemberProvider;

    @Operation(summary = "소셜 로그인", description = SOCIAL_LOGIN_DESCRIPTION)
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "provider별 소셜 토큰을 전달합니다.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthRequestDTO.SocialLoginRequest.class),
                    examples = {
                            @ExampleObject(name = "Kakao", value = KAKAO_REQUEST_EXAMPLE),
                            @ExampleObject(name = "Google", value = GOOGLE_REQUEST_EXAMPLE),
                            @ExampleObject(name = "Apple", value = APPLE_REQUEST_EXAMPLE)
                    }
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "소셜 로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = SOCIAL_LOGIN_SUCCESS_EXAMPLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 provider",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INVALID_PROVIDER_EXAMPLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 소셜 토큰",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INVALID_SOCIAL_TOKEN_EXAMPLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "OAuth 설정값 누락",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = OAUTH_CONFIG_MISSING_EXAMPLE)
                    )
            )
    })
    @PostMapping("/login/{provider}")
    public ApiResponse<AuthResponseDTO.SocialLoginResponse> loginWithSocial(
            @Parameter(
                    name = "provider",
                    description = "소셜 로그인 제공자",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "kakao",
                    schema = @Schema(allowableValues = {"kakao", "google", "apple"})
            )
            @PathVariable String provider,
            @Valid @RequestBody AuthRequestDTO.SocialLoginRequest request
    ) {
        return ApiResponse.onSuccess(authCommandService.loginWithSocial(OAuthProvider.from(provider), request));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 검증한 뒤 Access Token과 Refresh Token을 재발급합니다.")
    @SecurityRequirements
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = TOKEN_REISSUE_SUCCESS_EXAMPLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 Refresh Token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INVALID_REFRESH_TOKEN_EXAMPLE)
                    )
            )
    })
    @PostMapping("/reissue")
    public ApiResponse<AuthResponseDTO.TokenResponse> reissueToken(
            @Parameter(
                    name = X_REFRESH_TOKEN_HEADER,
                    description = "서버에서 발급받은 Refresh Token",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "jwt_refresh_token"
            )
            @RequestHeader(value = X_REFRESH_TOKEN_HEADER, required = false) String refreshToken
    ) {
        return ApiResponse.onSuccess(authCommandService.reissueToken(refreshToken));
    }

    @Operation(summary = "로그아웃", description = "현재 로그인한 회원의 Refresh Token을 무효화합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "무효화할 Refresh Token을 전달합니다.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthRequestDTO.LogoutRequest.class),
                    examples = @ExampleObject(value = LOGOUT_REQUEST_EXAMPLE)
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = LOGOUT_SUCCESS_EXAMPLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 또는 유효하지 않은 Refresh Token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INVALID_REFRESH_TOKEN_EXAMPLE)
                    )
            )
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody AuthRequestDTO.LogoutRequest request
    ) {
        authCommandService.logout(currentMemberProvider.getCurrentMemberId(), request);
        return ApiResponse.onSuccess(null);
    }
}
