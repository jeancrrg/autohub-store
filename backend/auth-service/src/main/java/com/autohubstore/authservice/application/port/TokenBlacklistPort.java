package com.autohubstore.authservice.application.port;

/**
 * Port para blacklist de access tokens no Redis.
 * Chave: token:blacklist:{jti} | TTL = tempo residual do token.
 */
public interface TokenBlacklistPort {

    void blacklist(String jti, long ttlSeconds);

    boolean isBlacklisted(String jti);

}
