package com.example.ForDay.domain.app.dto.response;

import com.example.ForDay.domain.app.type.Platform;
import com.example.ForDay.domain.app.type.UpdateType;

public record VersionPolicyResDto(
        int policyVersion,
        Platform platform,
        Current current,
        Threshold minSupported,
        Threshold latest,
        UpdateType update,
        String storeUrl,
        String message
) {
    public record Current(String version, int build) {}
    public record Threshold(String version, int build) {}
}