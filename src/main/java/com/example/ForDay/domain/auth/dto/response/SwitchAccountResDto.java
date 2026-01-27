package com.example.ForDay.domain.auth.dto.response;

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
}
