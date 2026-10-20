package com.bcp.workshop.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@ApplicationScoped
public class LegacyTokenUtil {
    static final long TTL_SECONDS = 3600;

    @ConfigProperty(name = "auth.token.secret")
    String secret;

    public String create(String username, String role) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("AUTH_TOKEN_SECRET must be at least 32 bytes");
        }

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .claim("role", role)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(TTL_SECONDS)))
                .build();
        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            token.sign(new MACSigner(secretBytes));
            return token.serialize();
        } catch (JOSEException error) {
            throw new IllegalStateException("Unable to issue authentication token", error);
        }
    }
}
