package com.moru.server.domain.member.service.command.auth;

import com.moru.server.domain.member.dto.AuthRequestDTO;
import com.moru.server.domain.member.dto.AuthResponseDTO;

public interface AuthCommandService {

    AuthResponseDTO.TokenResponse issueDevToken(AuthRequestDTO.DevTokenRequest request);
}
