package com.example.ForDay.domain.auth.dto.response;

import com.example.ForDay.domain.auth.dto.request.SwitchAccountReqDto;
import com.example.ForDay.domain.user.type.SocialType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SwitchAccountResDto {
    private SocialType socialType;
    private String accessToken;
    private String refreshToken;
    private String fcmToken;

    public static SwitchAccountResDto of(SocialType socialType, String accessToken, String refreshToken, String fcmToken) {
        return new SwitchAccountResDto(socialType, accessToken, refreshToken, fcmToken);
    }
}
