package com.moru.server.global.security.auth;

import com.moru.server.domain.member.entity.enums.Role;

public record AuthenticatedMember(
        Long memberId,
        Role role
) {
}
