package com.tissue.workspace.domain.service;

import java.security.SecureRandom;

public final class WorkspaceKeyGenerator {

    private WorkspaceKeyGenerator() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    private static final int KEY_LENGTH = 8;
    private static final String WORKSPACE_KEY_PREFIX = "WS-";

    private static final char[] BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateWorkspaceKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH + WORKSPACE_KEY_PREFIX.length());
        sb.append(WORKSPACE_KEY_PREFIX);

        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(BASE62[RANDOM.nextInt(BASE62.length)]);
        }

        return sb.toString();
    }
}
