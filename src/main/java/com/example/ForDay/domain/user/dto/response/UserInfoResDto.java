package com.example.ForDay.domain.user.dto.response;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.infra.s3.util.S3Util;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "사용자 마이페이지 정보 응답 DTO")
public class UserInfoResDto {
    @Schema(description = "프로필 이미지 URL", example = "https://forday-s3.amazonaws.com/profiles/user1.png")
    private String profileImageUrl;

    @Schema(description = "사용자 닉네임", example = "포데이러버")
    private String nickname;

    @Schema(description = "총 수집한 스티커 개수", example = "42")
    private Integer totalCollectedStickerCount;

    private boolean unReadNotificationExists;

    public static UserInfoResDto of(User user, int totalStickerCount, S3Util s3Util, boolean unReadNotificationExists) {
        return new UserInfoResDto(
                s3Util.toProfileMainResizedUrl(user.getProfileImageUrl()),
                user.getNickname(),
                totalStickerCount,
                unReadNotificationExists
        );
    }
}
