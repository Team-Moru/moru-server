package com.moru.server.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.service.command.auth.AuthCommandService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.AuthenticatedMember;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String WITHDRAWAL_SUCCESS_EXAMPLE = """
            {
              "isSuccess": true,
              "code": "COMMON200",
              "message": "성공입니다.",
              "result": {
                "message": "회원 탈퇴가 완료되었습니다."
              }
            }
            """;

    private static final String UNAUTHORIZED_EXAMPLE = """
            {
              "isSuccess": false,
              "code": "COMMON401",
              "message": "인증이 필요합니다."
            }
            """;

    private final AuthCommandService authCommandService;

    @Operation(summary = "회원탈퇴", description = "현재 로그인한 회원의 계정과 관련 데이터를 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = WITHDRAWAL_SUCCESS_EXAMPLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)
                    )
            )
    })
    @DeleteMapping("/withdrawal")
    public ApiResponse<AuthResponseDTO.WithdrawalResponse> withdraw(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(authCommandService.withdraw(member.memberId()));
    }
}
