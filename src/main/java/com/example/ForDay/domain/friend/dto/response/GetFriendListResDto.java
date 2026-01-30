package com.example.ForDay.domain.friend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetFriendListResDto {
    private String message;
    private List<UserInfoDto> userInfo;
    private String lastUserId;
    private boolean hasNext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDto {
        private String userId;
        private String nickname;
        private String profileImageUrl;
    }
}
