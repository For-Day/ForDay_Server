package com.example.ForDay.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResDto {
    private String profileImageUrl;
    private String nickname;
    private Integer totalCollectedStickerCount;
}
