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
 * Supplies HMAC verification key directly on {@link JWTAuthContextInfo} from
 * {@code JWT_SECRET}. Avoids SmallRye's oct-JWK {@code key.location} path.
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
        contextInfo.setRelaxVerificationKeyValidation(true);

        Log.infof(
                "HMAC JWTAuthContextInfo active: issuer=%s secretBytes=%d secretVerificationKey=SET",
                issuer,
                secretBytes.length);
        return contextInfo;
    }
}
