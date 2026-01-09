package com.example.ForDay.domain.auth.service;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;

import com.example.ForDay.domain.auth.dto.response.ApplePublicKeyDto;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.LocatorAdapter;

public class MyKeyLocator extends LocatorAdapter<Key> {

    private final List<ApplePublicKeyDto.Key> publicKeys;

    public MyKeyLocator(List<ApplePublicKeyDto.Key> publicKeys) {
        this.publicKeys = publicKeys;
    }

    @Override
    protected Key locate(JwsHeader header) {

        ApplePublicKeyDto.Key matchedKey = publicKeys.stream()
                .filter(key ->
                        key.getKid().equals(header.getKeyId()) &&
                                key.getAlg().equals(header.getAlgorithm())
                )
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "일치하는 Apple public key 없음. kid=" + header.getKeyId()
                        )
                );

        BigInteger n = new BigInteger(1,
                Base64.getUrlDecoder().decode(matchedKey.getN()));
        BigInteger e = new BigInteger(1,
                Base64.getUrlDecoder().decode(matchedKey.getE()));

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new RSAPublicKeySpec(n, e);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception ex) {
            throw new RuntimeException("[애플 로그인] public key 생성 실패", ex);
        }
    }
}
