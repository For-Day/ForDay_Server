package com.example.ForDay.domain.auth.dto;

import com.example.ForDay.domain.auth.dto.response.OnboardingDataDto;

public record LoginInternalResult(
        String accessToken,
        String refreshToken,
        boolean onboardingCompleted,
        boolean isNicknameSet,
        OnboardingDataDto onboardingData
) {}