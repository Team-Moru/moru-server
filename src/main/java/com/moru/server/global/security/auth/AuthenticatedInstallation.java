package com.moru.server.global.security.auth;

import java.util.UUID;

public record AuthenticatedInstallation(UUID id) {
    public static final String AUTHORITY = "ROLE_ANONYMOUS_INSTALLATION";
}
