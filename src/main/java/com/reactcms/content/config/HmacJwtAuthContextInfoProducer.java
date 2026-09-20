package com.reactcms.content.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Supplies HMAC verification key directly on {@link JWTAuthContextInfo}.
 * <p>
 * SmallRye's {@code JWTAuthContextInfoProvider#getContextInfo()} calls
 * {@code getOptionalContextInfo()} on itself, so an Alternative for
 * {@code Optional<JWTAuthContextInfo>} is ignored. We must Alternative the
 * plain {@link JWTAuthContextInfo} bean instead.
 * <p>
 * Also avoids loading oct-JWK via {@code setPublicKeyContent}, which yields
 * {@code Verification key is unresolvable} for HS256 tokens on this service.
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class HmacJwtAuthContextInfoProducer {

    @ConfigProperty(name = "react-cms.jwt.hmac-secret")
    String hmacSecret;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @Produces
    @Singleton
    @Alternative
    @Priority(1)
    public JWTAuthContextInfo produceJwtAuthContextInfo() {
        byte[] secretBytes = hmacSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");

        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        contextInfo.setSecretVerificationKey(secretKey);
        contextInfo.setIssuedBy(issuer);
        contextInfo.setSignatureAlgorithm(Set.of(SignatureAlgorithm.HS256));
        contextInfo.setTokenSchemes(List.of("Bearer"));
        contextInfo.setRequireNamedPrincipal(true);
        // Keep validation relaxed for HMAC secret keys (jose4j length checks).
        contextInfo.setRelaxVerificationKeyValidation(true);

        Log.infof(
                "HMAC JWTAuthContextInfo active: issuer=%s secretBytes=%d secretVerificationKey=SET relax=true",
                issuer,
                secretBytes.length);
        return contextInfo;
    }
}
