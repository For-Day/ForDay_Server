package com.example.ForDay.domain.app.service;

import com.example.ForDay.domain.app.dto.response.VersionPolicyResDto;
import com.example.ForDay.domain.app.entity.AppVersion;
import com.example.ForDay.domain.app.repository.AppVersionRepository;
import com.example.ForDay.domain.app.type.Platform;
import com.example.ForDay.domain.app.type.UpdateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppServiceTest {

    @Mock
    private AppVersionRepository appVersionRepository;

    @InjectMocks
    private AppService appService;

    @Test
    @DisplayName("서비스 차단 모드(BLOCK)가 활성화되어 있으면 최우선으로 BLOCK을 반환한다")
    void getPolicy_BlockEnabled() {
        // given
        AppVersion policyEntity = createBasePolicy()
                .blockEnabled(true)
                .blockMessage("서버 점검 중")
                .build();
        given(appVersionRepository.findFirstByPlatformOrderByCreatedAtDesc(Platform.IOS))
                .willReturn(Optional.of(policyEntity));

        // when
        VersionPolicyResDto result = appService.getPolicy(Platform.IOS, "1.5.0", 150);

        // then
        assertThat(result.update()).isEqualTo(UpdateType.BLOCK);
        assertThat(result.message()).isEqualTo("서버 점검 중");
    }

    @Test
    @DisplayName("현재 버전이 최소 지원 버전보다 낮으면 FORCE(강제 업데이트)를 반환한다")
    void getPolicy_ForceUpdate() {
        // given: 최소 지원 1.1.0(110)
        AppVersion policyEntity = createBasePolicy()
                .minSupportedVersion("1.1.0").minSupportedBuild(110)
                .latestVersion("1.2.0").latestBuild(120)
                .forceMessage("강제 업데이트 필요")
                .build();
        given(appVersionRepository.findFirstByPlatformOrderByCreatedAtDesc(Platform.ANDROID))
                .willReturn(Optional.of(policyEntity));

        // when: 현재 1.0.9(109)
        VersionPolicyResDto result = appService.getPolicy(Platform.ANDROID, "1.0.9", 109);

        // then
        assertThat(result.update()).isEqualTo(UpdateType.FORCE);
        assertThat(result.message()).isEqualTo("강제 업데이트 필요");
    }

    @Test
    @DisplayName("현재 버전이 최신 버전보다 낮으면 RECOMMEND(권장 업데이트)를 반환한다")
    void getPolicy_RecommendUpdate() {
        // given: 최신 1.2.0(120)
        AppVersion policyEntity = createBasePolicy()
                .minSupportedVersion("1.0.0").minSupportedBuild(100)
                .latestVersion("1.2.0").latestBuild(120)
                .recommendMessage("업데이트 권장")
                .build();
        given(appVersionRepository.findFirstByPlatformOrderByCreatedAtDesc(Platform.IOS))
                .willReturn(Optional.of(policyEntity));

        // when: 현재 1.1.0(110)
        VersionPolicyResDto result = appService.getPolicy(Platform.IOS, "1.1.0", 110);

        // then
        assertThat(result.update()).isEqualTo(UpdateType.RECOMMEND);
        assertThat(result.message()).isEqualTo("업데이트 권장");
    }

    @Test
    @DisplayName("현재 버전이 최신 버전과 같거나 높으면 NONE을 반환한다")
    void getPolicy_None() {
        // given
        AppVersion policyEntity = createBasePolicy()
                .latestVersion("1.2.0").latestBuild(120)
                .build();
        given(appVersionRepository.findFirstByPlatformOrderByCreatedAtDesc(Platform.IOS))
                .willReturn(Optional.of(policyEntity));

        // when: 현재 1.2.0(120)
        VersionPolicyResDto result = appService.getPolicy(Platform.IOS, "1.2.0", 120);

        // then
        assertThat(result.update()).isEqualTo(UpdateType.NONE);
        assertThat(result.message()).isEmpty();
    }

    private AppVersion.AppVersionBuilder createBasePolicy() {
        return AppVersion.builder()
                .policyVersion(1)
                .platform(Platform.IOS)
                .minSupportedVersion("1.0.0").minSupportedBuild(100)
                .latestVersion("1.0.0").latestBuild(100)
                .storeUrl("https://store.url")
                .forceMessage("F")
                .recommendMessage("R")
                .blockEnabled(false);
    }
}