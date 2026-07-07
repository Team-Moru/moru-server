package com.moru.server.global.security.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@Component
public class CurrentMemberProvider {

    public Long getCurrentMemberId() {
        return getCurrentMember().memberId();
    }

    public Role getCurrentMemberRole() {
        return getCurrentMember().role();
    }

    public AuthenticatedMember getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthenticatedMember authenticatedMember)) {
            throw new BusinessException(ErrorStatus.UNAUTHORIZED);
        }

        return authenticatedMember;
    }
}
