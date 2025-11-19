package com.gpt.geumpumtabackend.global.oauth.apple;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleClientSecretGenerator {
    @Value("${apple.team-id}")
    private String teamId;

    @Value("${apple.client-id}")
    private String clientId;

    @Value("${apple.key-id}")
    private String keyId;

    @Value("${apple.private-key}")
    private String privateKeyPem;

    @Value("${apple.audience}")
    private String audience;

    public String generate() {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPem);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256) // 키 타입에 맞게
                    .keyID(keyId)
                    .type(JOSEObjectType.JWT)
                    .build();

            Instant now = Instant.now();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(teamId)
                    .subject(clientId)
                    .audience("https://appleid.apple.com")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(60 * 60))) // 1시간짜리 client_secret
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Apple client_secret", e);
        }
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("EC").generatePrivate(keySpec); // 필요 시 "RSA"로 변경
    }
}
