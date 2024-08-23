package org.bayaweaver.codeartifactproxy;

import java.time.Instant;

class AuthorizationTokenCache {
    private static final AuthorizationTokenCache instance = new AuthorizationTokenCache();

    private Instant expiration;
    private String token;

    private AuthorizationTokenCache() {}

    static AuthorizationTokenCache instance() {
        return instance;
    }

    void put(String token, Instant expiration) {
        this.token = token;
        this.expiration = expiration;
    }

    String get() {
        if (expiration == null || expiration.isBefore(Instant.now())) {
            token = null;
        }
        return token;
    }

    void evict() {
        token = null;
    }
}