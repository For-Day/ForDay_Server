package com.example.ForDay.domain.app.utils;

import java.util.Objects;

public final class AppVersionUtil implements Comparable<AppVersionUtil> {
    private final int major;
    private final int minor;
    private final int patch;
    private final int build;

    private AppVersionUtil(int major, int minor, int patch, int build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
    }

    /**
     * 문자열 버전(x.y.z)과 빌드 번호를 받아 AppVersion 객체를 생성합니다.
     */
    public static AppVersionUtil of(String semver, int build) {
        Objects.requireNonNull(semver, "Version string must not be null");

        // 정규식 . 을 기준으로 분리 (1.2.0 -> [1, 2, 0])
        String[] parts = semver.trim().split("\\.");

        // 유연한 처리를 위해 부족한 부분은 0으로 채움 (ex: 1.1 -> 1.1.0)
        int major = parts.length > 0 ? parsePart(parts[0]) : 0;
        int minor = parts.length > 1 ? parsePart(parts[1]) : 0;
        int patch = parts.length > 2 ? parsePart(parts[2]) : 0;

        return new AppVersionUtil(major, minor, patch, Math.max(0, build));
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", "")); // 숫자만 남기고 파싱
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 버전 비교 로직 (Major -> Minor -> Patch -> Build 순서)
     */
    @Override
    public int compareTo(AppVersionUtil other) {
        if (this.major != other.major) return Integer.compare(this.major, other.major);
        if (this.minor != other.minor) return Integer.compare(this.minor, other.minor);
        if (this.patch != other.patch) return Integer.compare(this.patch, other.patch);
        return Integer.compare(this.build, other.build);
    }

    public String versionString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    public int build() {
        return build;
    }
}