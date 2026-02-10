package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.entity.RefreshToken;
import com.example.ForDay.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // username -> socialId로 사용
    public void save(String username, String refreshToken) {
        refreshTokenRepository.save(new RefreshToken(username, refreshToken));
    }

    public String get(String username) {
        return refreshTokenRepository.findById(username)
                .map(RefreshToken::getToken)
                .orElse(null);
    }
}

